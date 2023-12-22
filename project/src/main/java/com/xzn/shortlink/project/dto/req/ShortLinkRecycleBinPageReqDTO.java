package com.xzn.shortlink.project.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzn.shortlink.project.dao.entity.ShortLinkDO;
import java.util.List;
import lombok.Data;

/**
 * @author Nruonan
 * @description 短链接分页请求参数
 */
@Data
public class ShortLinkRecycleBinPageReqDTO extends Page<ShortLinkDO> {
    /**
     * a分页标识
     */
    private List<String> gidList;
}
