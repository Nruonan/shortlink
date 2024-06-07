package com.xzn.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzn.shortlink.project.common.biz.user.UserContext;
import com.xzn.shortlink.project.common.convention.exception.ServiceException;
import com.xzn.shortlink.project.dao.entity.GroupDO;
import com.xzn.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.xzn.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkDeviceStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.xzn.shortlink.project.dao.entity.LinkNetworkStatsDO;
import com.xzn.shortlink.project.dao.mapper.LinkAccessLogsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkBrowserStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkDeviceStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkGroupMapper;
import com.xzn.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkNetworkStatsMapper;
import com.xzn.shortlink.project.dao.mapper.LinkOsStatsMapper;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.xzn.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsAccessDailyRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsBrowserRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsDeviceRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsLocaleCNRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsNetworkRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsOsRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsTopIpRespDTO;
import com.xzn.shortlink.project.dto.resp.ShortLinkStatsUvRespDTO;
import com.xzn.shortlink.project.service.ShortLinkStatsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * @description
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {
    private final LinkGroupMapper linkGroupMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(requestParam);
        if(CollUtil.isEmpty(listStatsByShortLink)){
            return null;
        }
        // 基础访问数据
        LinkAccessStatsDO pvUvUidStatsByShortLink = linkAccessLogsMapper.findPvUvUidStatsByShortLink(requestParam);
        // 基础访问情况
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        // 获取日期
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()),
                DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH)
            .stream()
            .map(DateUtil::formatDate)
            .toList();

        // 根据日期查询pv uv uip
        rangeDates.forEach(each -> listStatsByShortLink.stream()
            .filter(item -> Objects.equals(each , DateUtil.formatDate(item.getDate())))
            .findFirst()
            .ifPresentOrElse(item ->{
                // 存在则赋值
                ShortLinkStatsAccessDailyRespDTO linkStatsAccessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(each)
                    .pv(item.getPv())
                    .uv(item.getUv())
                    .uip(item.getUip())
                    .build();
                daily.add(linkStatsAccessDailyRespDTO);
            },() -> {
                // 不存在则为0
                ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(each)
                    .pv(0)
                    .uv(0)
                    .uip(0)
                    .build();
                daily.add(accessDailyRespDTO);
            }));

        //根据地区访问
        List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocaleByShortLink = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);
        // 得到总访问量
        int localeSum = listedLocaleByShortLink.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                    .sum();
        listedLocaleByShortLink.forEach(each -> {
            // 得到比重
            double ratio = (double)each.getCnt() / localeSum;
            // 得到小数的比重
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                .locale(each.getProvince())
                .cnt(each.getCnt())
                .ratio(actualRatio)
                .build();
            localeCnStats.add(localeCNRespDTO);
        });

        // 小时访问
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> listHourStatsByShortLink = linkAccessStatsMapper.listHourStatsByShortLink(requestParam);
        for(int i = 0; i < 24; i++){
            AtomicInteger hour = new AtomicInteger(i);
            // 计算小时访问次数
            int hourCount = listHourStatsByShortLink.stream()
                .filter(each -> Objects.equals(each.getHour(), hour.get()))
                .findFirst()
                .map(LinkAccessStatsDO::getPv)
                .orElse(0);
            hourStats.add(hourCount);
        }

        // 高频访问IP详情
        List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(requestParam);
        listTopIpByShortLink.forEach(each ->{
            ShortLinkStatsTopIpRespDTO linkStatsTopIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                .cnt(Integer.valueOf(each.get("count").toString()))
                .ip(each.get("ip").toString())
                .build();
            topIpStats.add(linkStatsTopIpRespDTO);
        });

        // 一周访问详情
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> listWeekdayStatsByShortLink = linkAccessStatsMapper.listWeekdayStatsByShortLink(requestParam);
        for(int i = 1; i < 8; i++){
            AtomicInteger weekday = new AtomicInteger(i);
            Integer weekdayCount = listWeekdayStatsByShortLink.stream()
                .filter(each -> Objects.equals(each.getWeekday(), weekday.get()))
                .findFirst()
                .map(LinkAccessStatsDO::getPv)
                .orElse(0);
            weekdayStats.add(weekdayCount);
        }
        // 浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByShortLink = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        int browserSum = listBrowserStatsByShortLink.stream()
            .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
            .sum();
        listBrowserStatsByShortLink.forEach(each ->{
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / browserSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO statsBrowserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                .browser(each.get("browser").toString())
                .cnt(Integer.parseInt(each.get("count").toString()))
                .ratio(actualRatio)
                .build();
            browserStats.add(statsBrowserRespDTO);
        });

        // 操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        int osSum = listOsStatsByShortLink.stream()
            .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
            .sum();
        listOsStatsByShortLink.forEach(each ->{
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO statsOsRespDTO = ShortLinkStatsOsRespDTO.builder()
                .os(each.get("os").toString())
                .ratio(actualRatio)
                .cnt(Integer.valueOf(each.get("count").toString()))
                .build();
            osStats.add(statsOsRespDTO);
        });

        // 访客访问类型详情
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Integer.parseInt(
            Optional.ofNullable(findUvTypeByShortLink)
                .map(each -> each.get("oldUserCnt"))
                .map(Object::toString)
                .orElse("0")
        );
        int newUserCnt = Integer.parseInt(
            Optional.ofNullable(findUvTypeByShortLink)
                .map(each -> each.get("newUserCnt"))
                .map(Object::toString)
                .orElse("0")
        );
        int uvSum = oldUserCnt + newUserCnt;
        double oldRatio = (double) oldUserCnt / uvSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = (double) newUserCnt / uvSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        ShortLinkStatsUvRespDTO newUser = ShortLinkStatsUvRespDTO.builder()
            .ratio(actualNewRatio)
            .cnt(newUserCnt)
            .uvType("newUser")
            .build();
        uvTypeStats.add(newUser);
        ShortLinkStatsUvRespDTO oldUser = ShortLinkStatsUvRespDTO.builder()
            .ratio(actualOldRatio)
            .cnt(oldUserCnt)
            .uvType("oldUser")
            .build();
        uvTypeStats.add(oldUser);

        // 访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int deviceSum = listDeviceStatsByShortLink.stream()
            .mapToInt(LinkDeviceStatsDO::getCnt)
            .sum();
        listDeviceStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                .cnt(each.getCnt())
                .device(each.getDevice())
                .ratio(actualRatio)
                .build();
            deviceStats.add(deviceRespDTO);
        });

        // 访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
            .mapToInt(LinkNetworkStatsDO::getCnt)
            .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                .cnt(each.getCnt())
                .network(each.getNetwork())
                .ratio(actualRatio)
                .build();
            networkStats.add(networkRespDTO);
        });
        return ShortLinkStatsRespDTO.builder()
            .pv(pvUvUidStatsByShortLink.getPv())
            .uv(pvUvUidStatsByShortLink.getUv())
            .uip(pvUvUidStatsByShortLink.getUip())
            .daily(daily)
            .osStats(osStats)
            .deviceStats(deviceStats)
            .hourStats(hourStats)
            .localeCnStats(localeCnStats)
            .topIpStats(topIpStats)
            .networkStats(networkStats)
            .uvTypeStats(uvTypeStats)
            .weekdayStats(weekdayStats)
            .browserStats(browserStats)
            .build();
    }

    @Override
    public ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        List<LinkAccessStatsDO> listStatsByGroup = linkAccessStatsMapper.listStatsByGroup(requestParam);
        if(CollUtil.isEmpty(listStatsByGroup)){
            return null;
        }
        // 基础访问数据
        LinkAccessStatsDO pvUvUidStatsByGroup = linkAccessLogsMapper.findPvUvUidStatsByGroup(requestParam);
        // 基础访问情况
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        // 获取日期
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()),
                DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH)
            .stream()
            .map(DateUtil::formatDate)
            .toList();
        // 根据日期查询pv uv uip
        rangeDates.forEach(each -> listStatsByGroup.stream()
            .filter(item -> Objects.equals(each , DateUtil.formatDate(item.getDate())))
            .findFirst()
            .ifPresentOrElse(item ->{
                // 存在则赋值
                ShortLinkStatsAccessDailyRespDTO linkStatsAccessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(each)
                    .pv(item.getPv())
                    .uv(item.getUv())
                    .uip(item.getUip())
                    .build();
                daily.add(linkStatsAccessDailyRespDTO);
            },() -> {
                // 不存在则为0
                ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(each)
                    .pv(0)
                    .uv(0)
                    .uip(0)
                    .build();
                daily.add(accessDailyRespDTO);
            }));

        //根据地区访问
        List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocaleByGroup = linkLocaleStatsMapper.listLocaleByGroup(requestParam);
        // 得到总访问量
        int localeSum = listedLocaleByGroup.stream()
            .mapToInt(LinkLocaleStatsDO::getCnt)
            .sum();
        listedLocaleByGroup.forEach(each -> {
            // 得到比重
            double ratio = (double)each.getCnt() / localeSum;
            // 得到小数的比重
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                .locale(each.getProvince())
                .cnt(each.getCnt())
                .ratio(actualRatio)
                .build();
            localeCnStats.add(localeCNRespDTO);
        });

        // 小时访问
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> listHourStatsByGroup = linkAccessStatsMapper.listHourStatsByGroup(requestParam);
        for(int i = 0; i < 24; i++){
            AtomicInteger hour = new AtomicInteger(i);
            // 计算小时访问次数
            int hourCount = listHourStatsByGroup.stream()
                .filter(each -> Objects.equals(each.getHour(), hour.get()))
                .findFirst()
                .map(LinkAccessStatsDO::getPv)
                .orElse(0);
            hourStats.add(hourCount);
        }

        // 高频访问IP详情
        List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByGroup = linkAccessLogsMapper.listTopIpByGroup(requestParam);
        listTopIpByGroup.forEach(each ->{
            ShortLinkStatsTopIpRespDTO linkStatsTopIpRespDTO = ShortLinkStatsTopIpRespDTO.builder()
                .cnt(Integer.valueOf(each.get("count").toString()))
                .ip(each.get("ip").toString())
                .build();
            topIpStats.add(linkStatsTopIpRespDTO);
        });

        // 一周访问详情
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> listWeekdayStatsByGroup = linkAccessStatsMapper.listWeekdayStatsByGroup(requestParam);
        for(int i = 1; i < 8; i++){
            AtomicInteger weekday = new AtomicInteger(i);
            Integer weekdayCount = listWeekdayStatsByGroup.stream()
                .filter(each -> Objects.equals(each.getWeekday(), weekday.get()))
                .findFirst()
                .map(LinkAccessStatsDO::getPv)
                .orElse(0);
            weekdayStats.add(weekdayCount);
        }
        // 浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByGroup = linkBrowserStatsMapper.listBrowserStatsByGroup(requestParam);
        int browserSum = listBrowserStatsByGroup.stream()
            .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
            .sum();
        listBrowserStatsByGroup.forEach(each ->{
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / browserSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO statsBrowserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                .browser(each.get("browser").toString())
                .cnt(Integer.parseInt(each.get("count").toString()))
                .ratio(actualRatio)
                .build();
            browserStats.add(statsBrowserRespDTO);
        });

        // 操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByGroup = linkOsStatsMapper.listOsStatsByGroup(requestParam);
        int osSum = listOsStatsByGroup.stream()
            .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
            .sum();
        listOsStatsByGroup.forEach(each ->{
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO statsOsRespDTO = ShortLinkStatsOsRespDTO.builder()
                .os(each.get("os").toString())
                .ratio(actualRatio)
                .cnt(Integer.valueOf(each.get("count").toString()))
                .build();
            osStats.add(statsOsRespDTO);
        });

        // 访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO>listDeviceStatsByGroup = linkDeviceStatsMapper.listDeviceStatsByGroup(requestParam);
        int deviceSum = listDeviceStatsByGroup.stream()
            .mapToInt(LinkDeviceStatsDO::getCnt)
            .sum();
        listDeviceStatsByGroup.forEach(each -> {
            double ratio = (double) each.getCnt()/ deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                .cnt(each.getCnt())
                .device(each.getDevice())
                .ratio(actualRatio)
                .build();
            deviceStats.add(deviceRespDTO);
        });

        // 访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByGroup = linkNetworkStatsMapper.listNetworkStatsByGroup(requestParam);
        int networkSum = listNetworkStatsByGroup.stream()
            .mapToInt(LinkNetworkStatsDO::getCnt)
            .sum();
        listNetworkStatsByGroup.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                .cnt(each.getCnt())
                .network(each.getNetwork())
                .ratio(actualRatio)
                .build();
            networkStats.add(networkRespDTO);
        });
        return ShortLinkStatsRespDTO.builder()
            .pv(pvUvUidStatsByGroup.getPv())
            .uv(pvUvUidStatsByGroup.getUv())
            .uip(pvUvUidStatsByGroup.getUip())
            .daily(daily)
            .osStats(osStats)
            .deviceStats(deviceStats)
            .hourStats(hourStats)
            .localeCnStats(localeCnStats)
            .topIpStats(topIpStats)
            .networkStats(networkStats)
            .weekdayStats(weekdayStats)
            .browserStats(browserStats)
            .build();
    }

    @SneakyThrows
    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> oneShortLinkStatsAccessRecord(
        ShortLinkStatsAccessRecordReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        // 查询短链接
        LambdaQueryWrapper<LinkAccessLogsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
            .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .between(LinkAccessLogsDO::getCreateTime, requestParam.getStartDate(), requestParam.getEndDate())
            .eq(LinkAccessLogsDO::getDelFlag, 0)
            .orderByDesc(LinkAccessLogsDO::getCreateTime);
        // 分页
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> actualResult = linkAccessLogsDOIPage.convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogsList = actualResult.getRecords().stream()
            .map(ShortLinkStatsAccessRecordRespDTO::getUser)
            .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectUvTypeByUsers(
            requestParam.getGid(),
            requestParam.getFullShortUrl(),
            requestParam.getEnableStatus() ,
            requestParam.getStartDate(),
            requestParam.getEndDate() ,
            userAccessLogsList
        );
        actualResult.getRecords().forEach(each ->{
            String uvType = uvTypeList.stream()
                    .filter(item -> Objects.equals(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType"))
                    .map(Objects::toString)
                    .orElse("旧访客");
            each.setUvType(uvType);
        });
        return actualResult;
    }


    @SneakyThrows
    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(
        ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        checkGroupBelongToUser(requestParam.getGid());
        // 分页
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectGroupPage(requestParam);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> actualResult = linkAccessLogsDOIPage.convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogsList = actualResult.getRecords().stream()
            .map(ShortLinkStatsAccessRecordRespDTO::getUser)
            .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectGroupUvTypeByUsers(
            requestParam.getGid(),
            requestParam.getStartDate() ,
            requestParam.getEndDate() ,
            userAccessLogsList
        );
        actualResult.getRecords().forEach(each ->{
            String uvType = uvTypeList.stream()
                .filter(item -> Objects.equals(each.getUser(), item.get("user")))
                .findFirst()
                .map(item -> item.get("uvType"))
                .map(Objects::toString)
                .orElse("旧访客");
            each.setUvType(uvType);
        });
        return actualResult;
    }
    public void checkGroupBelongToUser(String gid) throws ServiceException{
        String username = Optional.ofNullable(UserContext.getUsername())
            .orElseThrow(() -> new SecurityException("用户未登录"));
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
            .eq(GroupDO::getGid, gid)
            .eq(GroupDO::getUsername, username);
        List<GroupDO> groupDOS = linkGroupMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(groupDOS)){
            throw new ServiceException("用户信息与分组标识不匹配");
        }

    }
}
