package org.conch.consensus.poc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * PoC calculator instance 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */
public class PocCalculator implements Serializable {
    
    static PocCalculator inst = new PocCalculator();
    
    // poc score converter
    private static final BigInteger SCORE_MULTIPLIER = BigInteger.valueOf(125000L);

    // the final poc score should divide the PERCENT_DIVISOR
    private static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

    // 1T convert to KB unit
    private static final long ONE_T_IN_KB_UNIT = 1024*1024*1024L;

    // default weight table
    private volatile PocTxBody.PocWeightTable pocWeightTable = PocTxBody.PocWeightTable.defaultPocWeightTable();

    volatile int lastHeight = -1;

    public static void setCurWeightTable(PocTxBody.PocWeightTable weightTable, int height) {
        inst.pocWeightTable = weightTable;
        inst.lastHeight = height;
    }

    public static PocTxBody.PocWeightTable getCurWeightTable(){
        if(inst.pocWeightTable == null) {
            inst.pocWeightTable = PocTxBody.PocWeightTable.defaultPocWeightTable();
        }
        return inst.pocWeightTable;
    }
    
    private static BigInteger getWeight(PocTxBody.WeightTableOptions weightTableOptions){
        return BigInteger.valueOf(inst.pocWeightTable.getWeightMap().get(weightTableOptions.getValue()).longValue());
    }

    static void ssHoldCal(PocScore pocScore) {
        BigInteger ssHoldWeight = getWeight(PocTxBody.WeightTableOptions.SS_HOLD);
        pocScore.ssScore = ssHoldWeight.multiply(pocScore.ssScore).divide(PERCENT_DIVISOR);
    }
    
    
    private static BigInteger predefineNodeTypeLevel(Peer.Type peerType){
       return BigInteger.valueOf(inst.pocWeightTable.getNodeTypeTemplate().get(peerType.getCode()).longValue());
    }
    static void nodeTypeCal(PocScore pocScore,PocTxBody.PocNodeType nodeType) {
        BigInteger typeScore = BigInteger.ZERO;
        BigInteger typeWeight = getWeight(PocTxBody.WeightTableOptions.NODE_TYPE);
        if (nodeType.getType().equals(Peer.Type.FOUNDATION)) {
            typeScore = typeWeight.multiply(predefineNodeTypeLevel(Peer.Type.FOUNDATION)).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
            typeScore = typeWeight.multiply(predefineNodeTypeLevel(Peer.Type.COMMUNITY)).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.HUB)) {
            typeScore = typeWeight.multiply(predefineNodeTypeLevel(Peer.Type.HUB)).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.BOX)) {
            typeScore = typeWeight.multiply(predefineNodeTypeLevel(Peer.Type.BOX)).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
            typeScore = typeWeight.multiply(predefineNodeTypeLevel(Peer.Type.NORMAL)).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        }
        pocScore.nodeTypeScore = typeScore;
    }

    private static BigInteger predefineHardwareLevel(PocTxBody.DeviceLevels deviceLevels){
        return BigInteger.valueOf(inst.pocWeightTable.getHardwareConfigTemplate().get(deviceLevels.getLevel()).longValue());
    }

    private static BigInteger predefineNetworkLevel(PocTxBody.DeviceLevels deviceLevels){
        return BigInteger.valueOf(inst.pocWeightTable.getNetworkConfigTemplate().get(deviceLevels.getLevel()).longValue());
    }

    private static BigInteger predefinePerformanceLevel(PocTxBody.DeviceLevels deviceLevels){
        return BigInteger.valueOf(inst.pocWeightTable.getTxPerformanceTemplate().get(deviceLevels.getLevel()).longValue());
    }

    /**
     * calculate the disk capacity:
     * - 1TB = 1 score
     * - max capacity is @Constants.DISK_CAPACITY_MAX_TB
     */
    private static void hardwareCal(PocScore pocScore, long diskCapacity){
        BigInteger hardwareWeight = getWeight(PocTxBody.WeightTableOptions.HARDWARE_CONFIG);
        BigInteger hardwareScore = BigInteger.valueOf(diskCapacity / 1024 / 1024 / 1024);

        // disk capacity limit validation
        if(Conch.getHeight() > Constants.POC_CAL_ALGORITHM) {
            Double diskCapacityTBD = new Double(diskCapacity) / new Double(ONE_T_IN_KB_UNIT);
            long diskCapacityTB = 0;
            // valid min disk value is 1T, allow 5% precision lose
            // step is 1T if disk value larger than 1T
            // max disk value is Constants.DISK_CAPACITY_MAX_TB=96T
            if(diskCapacityTBD > 0.95 && diskCapacityTBD <= 1.0){
                diskCapacityTB = 1;
            } else if(diskCapacityTBD > 1.0 && diskCapacityTBD <= Constants.DISK_CAPACITY_MAX_TB){
                diskCapacityTB = diskCapacityTBD.longValue();
            } else if(diskCapacityTBD > Constants.DISK_CAPACITY_MAX_TB) {
                diskCapacityTB = Constants.DISK_CAPACITY_MAX_TB;
            }
            hardwareScore = BigInteger.valueOf(diskCapacityTB);
        }

        pocScore.hardwareScore = hardwareWeight.multiply(hardwareScore.multiply(SCORE_MULTIPLIER)).divide(PERCENT_DIVISOR);
    }

    static void nodeConfCal(PocScore pocScore, PocTxBody.PocNodeConf nodeConf) {

        BigInteger serverScore = BigInteger.ZERO, hardwareScore = BigInteger.ZERO , networkScore = BigInteger.ZERO , performanceScore = BigInteger.ZERO;
        BigInteger serverWeight = getWeight(PocTxBody.WeightTableOptions.SERVER_OPEN);
        Long[] openedServices = nodeConf.getSystemInfo().getOpenServices();
        if (openedServices != null && openedServices.length > 0) {

            for (Long serviceCode : openedServices) {
                BigInteger _scorePreDefined = BigInteger.valueOf(inst.pocWeightTable.getServerOpenTemplate().get(serviceCode).longValue());
                if(_scorePreDefined == null) continue;
                serverScore = serverScore.add(_scorePreDefined);
            }
            pocScore.serverScore = serverScore.multiply(serverWeight).multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        }

        BigInteger hardwareWeight = getWeight(PocTxBody.WeightTableOptions.HARDWARE_CONFIG);
        if (nodeConf.getSystemInfo().getCore() >= 8 && nodeConf.getSystemInfo().getAverageMHz() >= 3600 && nodeConf.getSystemInfo().getMemoryTotal() >= 15 && nodeConf.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
            hardwareScore = predefineHardwareLevel(PocTxBody.DeviceLevels.GOOD);
        } else if (nodeConf.getSystemInfo().getCore() >= 4 && nodeConf.getSystemInfo().getAverageMHz() >= 3100 && nodeConf.getSystemInfo().getMemoryTotal() >= 7 && nodeConf.getSystemInfo().getHardDiskSize() >= 1000) {
            hardwareScore = predefineHardwareLevel(PocTxBody.DeviceLevels.MIDDLE);
        } else if (nodeConf.getSystemInfo().getCore() >= 2 && nodeConf.getSystemInfo().getAverageMHz() >= 2400 && nodeConf.getSystemInfo().getMemoryTotal() >= 3 && nodeConf.getSystemInfo().getHardDiskSize() >= 100) {
            hardwareScore = predefineHardwareLevel(PocTxBody.DeviceLevels.BAD);
        }
        pocScore.hardwareScore = hardwareWeight.multiply(hardwareScore.multiply(SCORE_MULTIPLIER)).divide(PERCENT_DIVISOR);

        BigInteger networkWeight = getWeight(PocTxBody.WeightTableOptions.NETWORK_CONFIG);
        if (nodeConf.getSystemInfo().isHadPublicIp()) {
            if (nodeConf.getSystemInfo().getBandWidth() >= 10) {
                networkScore = predefineNetworkLevel(PocTxBody.DeviceLevels.GOOD);
            } else if (nodeConf.getSystemInfo().getBandWidth() >= 5) {
                networkScore = predefineNetworkLevel(PocTxBody.DeviceLevels.MIDDLE);
            } else if (nodeConf.getSystemInfo().getBandWidth() >= 1) {
                networkScore = predefineNetworkLevel(PocTxBody.DeviceLevels.BAD);
            }
        } else {
            networkScore = predefineNetworkLevel(PocTxBody.DeviceLevels.POOR);
        }
        pocScore.networkScore = networkWeight.multiply(networkScore.multiply(SCORE_MULTIPLIER)).divide(PERCENT_DIVISOR);


        BigInteger performanceWeight = getWeight(PocTxBody.WeightTableOptions.TX_PERFORMANCE);
        if (nodeConf.getSystemInfo().getTradePerformance() >= 1000) {
            performanceScore = predefinePerformanceLevel(PocTxBody.DeviceLevels.GOOD);
        } else if (nodeConf.getSystemInfo().getTradePerformance() >= 500) {
            performanceScore = predefinePerformanceLevel(PocTxBody.DeviceLevels.MIDDLE);
        } else if (nodeConf.getSystemInfo().getTradePerformance() >= 300) {
            performanceScore = predefinePerformanceLevel(PocTxBody.DeviceLevels.BAD);
        }
        pocScore.performanceScore = performanceWeight.multiply(performanceScore.multiply(SCORE_MULTIPLIER)).divide(PERCENT_DIVISOR);
    }

    private static BigInteger predefineOnlineRateLevel(Peer.Type peerType,PocTxBody.OnlineStatusDef statusDef){
        return BigInteger.valueOf(inst.pocWeightTable.getOnlineRateTemplate().get(peerType.getCode()).get(statusDef.getValue()));
    }
    
    static void onlineRateCal(PocScore pocScore,Peer.Type nodeType, PocTxBody.PocOnlineRate onlineRate) {
        BigInteger onlineRateScore = BigInteger.ZERO;

        if (nodeType.equals(Peer.Type.FOUNDATION)) {
            if (onlineRate.getNetworkRate() >= 9900 && onlineRate.getNetworkRate() < 9999) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.FOUNDATION,PocTxBody.OnlineStatusDef.FROM_99_00_TO_99_99);
            } else if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.FOUNDATION,PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00);
            } else if (onlineRate.getNetworkRate() < 9700) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.FOUNDATION,PocTxBody.OnlineStatusDef.FROM_00_00_TO_97_00);
            }
        } else if (nodeType.equals(Peer.Type.COMMUNITY)) {
            if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.COMMUNITY,PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00);
            } else if (onlineRate.getNetworkRate() >= 9000 && onlineRate.getNetworkRate() < 9700) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.COMMUNITY,PocTxBody.OnlineStatusDef.FROM_90_00_TO_97_00);
            } else if (onlineRate.getNetworkRate() < 9000) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.COMMUNITY,PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00);
            }
        } else if (nodeType.equals(Peer.Type.HUB) || nodeType.equals(Peer.Type.BOX)) {
            if (onlineRate.getNetworkRate() >= 9900) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.HUB,PocTxBody.OnlineStatusDef.FROM_99_00_TO_100);
            } else if (onlineRate.getNetworkRate() >= 9700) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.HUB,PocTxBody.OnlineStatusDef.FROM_97_00_TO_100);
            } else if (onlineRate.getNetworkRate() < 9000) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.HUB,PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00);
            }
        } else if (nodeType.equals(Peer.Type.NORMAL)) {
            if (onlineRate.getNetworkRate() >= 9700) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.NORMAL,PocTxBody.OnlineStatusDef.FROM_97_00_TO_100);
            } else if (onlineRate.getNetworkRate() >= 9000) {
                onlineRateScore = predefineOnlineRateLevel(Peer.Type.NORMAL,PocTxBody.OnlineStatusDef.FROM_90_00_TO_100);
            }
        }
        pocScore.onlineRateScore = onlineRateScore.multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
    }

    static Map<Long,Integer> missBlockMap = new HashMap<>();
    
    private static BigInteger predefineblockMissLevel(PocTxBody.DeviceLevels deviceLevels){
        return BigInteger.valueOf(inst.pocWeightTable.getGenerationMissingTemplate().get(deviceLevels.getLevel()).longValue());
    }
    
    static void blockMissCal(PocScore pocScore,PocTxBody.PocGenerationMissing blockMiss) {
        Long accountId = pocScore.accountId;
        Integer missCount = 0;
        if(missBlockMap.containsKey(accountId)) missCount = missBlockMap.get(accountId);
        missCount++;

        BigInteger missBlcokScore = BigInteger.ZERO;
        /** 低: 1块/月 累积丢失数小于 3 , 中: 小于3块/周 累积丢失数小于 10 , 高: 大于3块每周 累积丢失数大于 10 
         *  只实现了累积的判断，还未实现一段时间丢失率的判断检查
         * */
        if(missCount <= 3){
            missBlcokScore = predefineblockMissLevel(PocTxBody.DeviceLevels.GOOD);
        }else if(missCount > 3 && missCount <= 10){
            missBlcokScore = predefineblockMissLevel(PocTxBody.DeviceLevels.MIDDLE);
        }else if(missCount > 10){
            missBlcokScore = predefineblockMissLevel(PocTxBody.DeviceLevels.BAD);
        }
        pocScore.blockMissScore = missBlcokScore.multiply(SCORE_MULTIPLIER).divide(PERCENT_DIVISOR);
        // TODO xy impl miss_block check in interval
    }

    static void bcCal(PocScore pocScore, Account account, PocTxBody.PocBcSpeed pocBcSpeed){
        //TODO un-impl now -20181230
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
