package com.xzn.shortlink.project.util;

import static com.xzn.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import java.util.Date;
import java.util.Optional;

/**
 * @author Nruonan
 * @description 短链接工具类
 */
public class LinkUtil {
     public static long getLinkCacheValidDate(Date validDate){
         return Optional.ofNullable(validDate)
             .map(each -> DateUtil.between(new Date(), each , DateUnit.MS))
             .orElse(DEFAULT_CACHE_VALID_TIME);
     }
}
