package com.sky.controller.admin;

import com.sky.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final RedisTemplate redisTemplate;

    /**
     * 修改店铺的营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status)
    {
        log.info("修改店铺的营业状态:{}", status);
        redisTemplate.opsForValue().set("SHOP_STATUS", status);
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus()
    {
        log.info("获取店铺营业状态");
        Integer shopStatus = Integer.valueOf(redisTemplate.opsForValue().get("SHOP_STATUS").toString());
        return Result.success(shopStatus);
    }

}
