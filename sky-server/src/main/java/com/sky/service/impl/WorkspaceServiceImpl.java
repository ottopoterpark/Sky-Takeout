package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    /**
     * 查询今日运营数据
     *
     * @return
     */
    @Override
    public BusinessDataVO businessData()
    {
        // 获取今日日期
        LocalDate today = LocalDate.now();

        // 获取今日新增用户
        Integer newUsers = Db.lambdaQuery(User.class)
                .eq(User::getCreateTime, today.atStartOfDay())
                .count()
                .intValue();

        // 查询今日所有订单
        List<Orders> orders = Db.lambdaQuery(Orders.class)
                .ge(Orders::getCheckoutTime, today.atStartOfDay())
                .list();
        Integer totalOrderCount = orders.size();

        // 获取今日有效订单
        List<Orders> validOrders = orders.stream().filter(o -> o.getStatus().equals(Orders.COMPLETED)).toList();
        Integer validOrderCount = validOrders.size();

        // 计算今日营业额
        Double turnover = validOrders
                .stream()
                .map(Orders::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (!totalOrderCount.equals(0))
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();

        // 计算平均客单价
        Double unitPrice = 0.0;
        if (!validOrderCount.equals(0))
            unitPrice = turnover / validOrderCount.doubleValue();

        // 返回结果
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }
}
