package com.xzn.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.remote.dto.req.RecycleBinSaveReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.xzn.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkGroupQueryRespDTO;
import com.xzn.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nruonan
 * @description 短链接中台远程调用服务
 */
public interface ShortLinkRemoteService {
    /**
     * 创建短链接
     */
    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }
    /**
     * 修改短链接
     */
    default Result<Void> updateShortLink(ShortLinkUpdateReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/update", JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }
    /**
     * 短链接分页查询
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }
    /**
     * 查询分组短链接总量
     */
    default Result<List<ShortLinkGroupQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam){
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestParam",requestParam);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    /**
     * 根据URL 获取网站标题
     */
    default Result<String> getTitleByUrl(String url){
        String resultStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/title?url=" + url);
        return JSON.parseObject(resultStr, new TypeReference<>() {
        });
    }
    /**
     * 将连接放入回收站
     */
    default void saveRecycleBin(RecycleBinSaveReqDTO requestParam){
        String resultStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/save",JSON.toJSONString(requestParam));

    }

    default Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam){
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gidList",requestParam.getGidList());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }
}
