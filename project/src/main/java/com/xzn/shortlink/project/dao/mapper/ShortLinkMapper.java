package com.xzn.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import com.xzn.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author Nruonan
 * @description 短链接持久层
 */
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    @Update("UPDATE t_link set total_pv = total_pv + #{totalPv} , total_uv = total_uv + #{totalUv} , total_uip = total_uip + #{totalUip} "
        + " where gid = #{gid} and full_short_url = #{fullShortUrl};")
    void incrementStats(@Param("gid") String gid,
        @Param("fullShortUrl") String fullShortUrl,
        @Param("totalPv")Integer totalPv,
        @Param("totalUv")Integer totalUv,
        @Param("totalUip")Integer totalUip
    );


    IPage<ShortLinkDO> pageLink( ShortLinkPageReqDTO requestParam);
}
