package com.xzn.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Nruonan
 * @description 短链接跳转实体
 */
@TableName("t_link_goto")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkGotoDO {
    /**
     * id
     */
    private Long id;
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
