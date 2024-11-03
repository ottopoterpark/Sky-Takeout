package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

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

    /**
     * 查询订单管理数据
     * @return
     */
    @Override
    public OrderOverViewVO overviewOrders()
    {
        // 获取所有订单
        List<Orders> orders = Db.lambdaQuery(Orders.class)
                .list();

        // 分别统计各种类型的订单
        Integer waitingOrders = (int) orders.stream().filter(o -> o.getStatus().equals(Orders.TO_BE_CONFIRMED)).count();
        Integer deliveredOrders = (int) orders.stream().filter(o -> o.getStatus().equals(Orders.CONFIRMED)).count();
        Integer completedOrders = (int) orders.stream().filter(o -> o.getStatus().equals(Orders.COMPLETED)).count();
        Integer cancelledOrders = (int) orders.stream().filter(o -> o.getStatus().equals(Orders.CANCELLED)).count();
        Integer allOrders = (int) orders.size();

        // 返回结果
        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     * @return
     */
    @Override
    public DishOverViewVO overviewDishes()
    {
        // 查询所有菜品
        List<Dish> dishes = Db.lambdaQuery(Dish.class)
                .list();

        // 分别统计启售和停售的菜品
        Integer sold = (int) dishes.stream().filter(d -> d.getStatus().equals(StatusConstant.ENABLE)).count();
        Integer discontinued = (int) dishes.stream().filter(d -> d.getStatus().equals(StatusConstant.DISABLE)).count();

        // 返回结果
        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     * @return
     */
    @Override
    public SetmealOverViewVO overviewSetmeals()
    {
        // 查询所有套餐
        List<Setmeal> setmeals = Db.lambdaQuery(Setmeal.class)
                .list();

        // 分别统计启售和停售的套餐
        Integer sold = (int) setmeals.stream().filter(s -> s.getStatus().equals(StatusConstant.ENABLE)).count();
        Integer discontinued = (int) setmeals.stream().filter(s -> s.getStatus().equals(StatusConstant.DISABLE)).count();

        // 返回结果
        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
