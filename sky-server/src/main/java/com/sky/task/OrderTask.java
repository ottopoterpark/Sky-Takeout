package com.sky.task;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTask {


    /**
     * 支付超时定时处理
     * （每分钟执行一次）
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void payTimeOut()
    {
        log.info("定时处理支付超时的订单:{}", LocalDateTime.now());

        // 查询待付款且下单时间在当前时间十五分钟前的订单
        Db.lambdaUpdate(Orders.class)
                .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusMinutes(15))
                .set(Orders::getStatus,Orders.CANCELLED)
                .set(Orders::getCancelReason,"订单支付超时，自动取消")
                .set(Orders::getCancelTime,LocalDateTime.now())
                .update();
    }

    /**
     * 接单超时定时处理（每分钟执行一次）
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void acceptTimeOut()
    {
        log.info("定时处理接单超时的订单:{}", LocalDateTime.now());

        // 查询正在等待接单且下单时间在当前时间十五分钟前的订单
        Db.lambdaUpdate(Orders.class)
                .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .lt(Orders::getCheckoutTime, LocalDateTime.now().minusMinutes(15))
                .set(Orders::getStatus,Orders.CANCELLED)
                .set(Orders::getPayStatus,Orders.REFUND)
                .set(Orders::getCancelReason,"商家接单超时，自动取消")
                .set(Orders::getCancelTime,LocalDateTime.now())
                .update();
    }

    /**
     * 派送超时定时处理（每分钟执行一次）
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void deliverTimeOut()
    {
        log.info("定时处理一直派送的订单:{}", LocalDateTime.now());

        // 查询处于派送中的订单
        Db.lambdaUpdate(Orders.class)
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusHours(2))
                .set(Orders::getStatus,Orders.COMPLETED)
                .update();
    }
}
