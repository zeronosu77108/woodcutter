package woodcutter.wc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class Tree{

    private final int MAX_LOG_AMOUNT = 200;
    private final int MAX_LEAVES_AMOUNT = 400;
    private final boolean tree;
    private final Material logType;
    private final Material leaveType;
    private final Material saplingType;
    private final int index;
    private final List<Block> treeLog = new ArrayList<>();
    private final List<Block> treeLeaves = new ArrayList<>();
    private final List<Location> firstLayerLoc = new ArrayList<>();

    /** 木を選択するコンストラクタ
     *
     * @param b 最初に破壊したブロック  原木でなければならない
     */
    public Tree(Block b) {
        logType = b.getType();
        index = WoodUtil.getIndex(b.getType());
        leaveType = WoodUtil.getLeavesMaterial(index);
        saplingType = WoodUtil.getSaplingMaterial(index);
        tree = isTree(b);
    }

    /** 木であるかどうかを返す
     *
     * @return 木：Ture 木でない:False
     */
    public boolean isTree(){
        return tree;
    }

    /** 選択したものが木であるかを判別し、返す
     *
     * @param b 最初に壊したブロック
     * @return 木：Ture 木でない:False
     */
    private boolean isTree(Block b) {
        Location under = b.getLocation().clone();
        under.setY(under.getY()-1);
        Material ub = under.getBlock().getType();
        under.setY(under.getY()-1);
        Material uub = under.getBlock().getType();

        //原木の下が土系のブロックか
        if(!isDirt(ub) && !isDirt(uub)){
            return false;
        }else if( !(WoodUtil.isWood(b.getType()) || WoodUtil.isMangroveLog(b.getType())) ){
            return false;
        }

        //原木と、隣接している葉をフィールドに入れていく
        int logIndex = WoodUtil.getIndex(b.getType());
        if(logIndex == 0){
            orkLogic(b, 4);
        }else if(WoodUtil.isMangroveLog(logIndex)){
            mangroveLogic(b);
        }else{
            orkLogic(b, 3);
        }

        //葉が原木に一つでも隣接していればTrueを返す
        return treeLeaves.size() > 0;
    }

    private boolean isDirt(Material type){
        return type.equals(Material.DIRT)
                || type.equals(Material.PODZOL)
                || type.equals(Material.COARSE_DIRT)
                || type.equals(Material.GRASS_BLOCK)
                || type.equals(Material.MYCELIUM)
                || type.equals(Material.CRIMSON_NYLIUM)
                || type.equals(Material.WARPED_NYLIUM)
                || type.equals(Material.NETHERRACK)
                || type.equals(Material.MOSS_BLOCK)
                || type.equals(Material.MUD)
                || type.equals(Material.MANGROVE_ROOTS)
                || type.equals(Material.MUDDY_MANGROVE_ROOTS);
    }

    /** 周囲のブロックを検査し、原木があればさらにその周りを検査する
     *
     * @param firstBlock 最初に壊したブロック  原木でなければならない
     */
    private void orkLogic(Block firstBlock, int radius) {
        Location l = firstBlock.getLocation().clone();
        boolean firstLayer = true;

        Location under = l.clone();
        under.setY(under.getY()-1);
        if (under.getBlock().getType().equals(logType)) {
            l = under.clone();
        }

        while (l.getBlock().getType().equals(logType)) {
            if(tooBig()){
                break;
            }else{
                searchAround(l, firstLayer, radius);
            }
            if(firstLayer) firstLayer = false;
            l.setY(l.getY()+1);
        }
    }

    /** 周囲のブロックを検査し、原木があればさらにその周りを検査する
     *
     * @param firstBlock 最初に壊したブロック  原木でなければならない
     */
    private void mangroveLogic(Block firstBlock) {
        Location l = firstBlock.getLocation().clone();
        boolean firstLayer = true;

        while (l.getBlock().getType().equals(logType)) {
            if(tooBig()){
                break;
            }else{
                searchAroundMangrove(l, firstLayer, 4);
            }
            if(firstLayer) firstLayer = false;
            l.setY(l.getY()+1);
        }
    }

    /** ある原木の周りを検査し、原木と葉をフィールドに追加する
     *
     * ある原木のxy周囲8マス、y座標1増加した9増マス分を検査し
     * 同種原木なら再帰、葉なら追加してreturnする
     *
     *
     * @param center 検査する原木の座標
     * @param firstLayer 地面に隣接している場所かどうか
     * @param radius 探索する範囲の半径(通常3)
     */
    private boolean searchAround(Location center, boolean firstLayer, int radius){
        if(tooBig()){
            return false;
        }
        Location l = center.clone();
        Block b = l.getBlock();

        if(b.getType().equals(logType)) {
            if(!treeLog.contains(b)) treeLog.add(b);
            if(firstLayer) firstLayerLoc.add(l.clone());
        }else if(b.getType().equals(leaveType)){
            if(!treeLeaves.contains(b)) treeLeaves.add(b);
            return true;
        }else{
            return true;
        }

        int sr = -radius + 2;
        int er = radius - 1;


        for(int h=0; h<2; h++) {
            for (int i = sr; i < er; i++) {
                for (int j = sr; j < er; j++) {
                    l.setX(center.getX() + i);
                    l.setZ(center.getZ() + j);
                    b = l.getBlock();

                    if (b.getType().equals(logType) && !treeLog.contains(b)) {
                        treeLog.add(b);
                        searchAround(l, firstLayer, radius);
                    } else if (b.getType().equals(leaveType) && !treeLeaves.contains(b)) {
                        treeLeaves.add(b);
                    }
                    if(tooBig()) return false;
                }
            }
            firstLayer = false;
            l.setY(center.getY()+1);
        }
        return true;
    }

    /** ある原木の周りを検査し、原木と葉をフィールドに追加する
     *
     * ある原木のxy周囲8マス、y座標1増加した9増マス分を検査し
     * 同種原木なら再帰、葉なら追加してreturnする
     *
     *
     * @param center 検査する原木の座標
     * @param firstBlock 地面に隣接している場所かどうか
     * @param diameter 探索する範囲の直径(通常3)
     */
    private boolean searchAroundMangrove(Location center, boolean firstBlock, int diameter){

        if(tooBig()){
            return false;
        }
        Location l = center.clone();
        Block b = l.getBlock();

        if(WoodUtil.isMangroveWood(b.getType())) {
            if(!treeLog.contains(b)) treeLog.add(b);
            if(firstBlock) firstLayerLoc.add(l.clone());
        }else if(b.getType().equals(leaveType)){
            if(!treeLeaves.contains(b)) treeLeaves.add(b);
            return true;
        }else{
            return true;
        }

        //reamke
        int width = diameter/2;
        for(int yShift=-1; yShift<2; yShift++){
            l.setY(center.getY()+yShift);
            for(int xShift=-width; xShift<=width; xShift++){
                for(int zShift=-width; zShift<=width; zShift++){
                    l.setX(center.getX() + xShift);
                    l.setZ(center.getZ() + zShift);
                    b = l.getBlock();
                    if (WoodUtil.isMangroveWood(b.getType()) && !treeLog.contains(b)) {
                        treeLog.add(b);
                        searchAroundMangrove(l, false, diameter);
                    } else if (b.getType().equals(leaveType) && !treeLeaves.contains(b)) {
                        treeLeaves.add(b);
                    }
                    if(tooBig()) return false;
                }
            }
        }

        return true;
    }

    /** 木を切る
     *
     * @param p 木を切ったPlayer
     * @return 実行できたかどうか(出来ればtrue)
     */
    public boolean cut(Player p){
        if(tooBig()) return false;
        ItemStack tool = p.getInventory().getItemInMainHand();
        new BukkitRunnable(){
            @Override
            public void run() {
                for(Block b : treeLog) b.breakNaturally(tool);
                for(Block b : treeLeaves) b.breakNaturally();
                for(Location l : firstLayerLoc) l.getBlock().setType(saplingType);
            }
        }.run();

        consumption(p, treeLog.size());
        return true;
    }

    /** 木の大きさが限度を超えているかどうか
     *
     * @return 木の大きさが限度を超えているか(超えていればtrue)
     */
    private boolean tooBig(){
        return treeLog.size() > MAX_LOG_AMOUNT || treeLeaves.size() > MAX_LEAVES_AMOUNT;
    }

    /** プレイヤーの手に持っているツールの耐久値を減らす
     *
     * @param p Player
     * @param value 減らす値
     */
    private void consumption(Player p, int value){
        ItemStack tool = p.getInventory().getItemInMainHand();
        int level = tool.getEnchantmentLevel(Enchantment.DURABILITY);
        //cf https://minecraft-ja.gamepedia.com/%E8%80%90%E4%B9%85%E5%8A%9B
        double decreaseProbability = (60.0+(40.0/(level+1.0))) / 100.0;

        short decrease = (short)(durability(tool) + (short)(value*decreaseProbability));
        if(tool.getType().getMaxDurability() == durability(tool)){
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 100, 1);
            p.spawnParticle(Particle.ITEM_CRACK, p.getLocation(), 40, tool);
            p.getInventory().setItemInMainHand(null);
            return;
        }else if(tool.getType().getMaxDurability() < decrease){
            decrease = tool.getType().getMaxDurability();
        }
        setDurability(tool, decrease);
    }

    private short durability(ItemStack item){
        ItemMeta meta = item.getItemMeta();
        return meta==null ? 0 : (short)((Damageable)meta).getDamage();
    }

    private void setDurability(ItemStack item, short durability){
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ((Damageable) meta).setDamage(durability);
            item.setItemMeta(meta);
        }
    }


}
