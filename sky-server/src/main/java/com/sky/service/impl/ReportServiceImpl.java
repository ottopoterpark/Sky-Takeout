package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.service.OrdersService;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end)
    {
        // 获得每一天的订单
        Map<LocalDate, List<Orders>> orders = Db.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.COMPLETED)
                .ge(Orders::getCheckoutTime, begin.atStartOfDay())
                .lt(Orders::getCheckoutTime, end.plusDays(1).atStartOfDay())
                .list()
                .stream()
                .collect(Collectors.groupingBy(o -> o.getCheckoutTime().toLocalDate()));

        // 得到范围里的每一天
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end))
        {
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        // 获得符合格式要求的日期字符串
        String dateList = StringUtils.join(dates, ",");

        // 获得营业额集合
        List<Double> turnovers=new ArrayList<>();
        dates.stream().forEach(d->{
            List<Orders> orderList = orders.get(d);
            if(orderList==null)
            {
                turnovers.add(0.0);
            }
            if (orderList!=null)
            {
                double sum = orderList
                        .stream()
                        .map(Orders::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .doubleValue();
                turnovers.add(sum);
            }
        });

        // 获得符合要求的营业额字符串
        String turnoverList = StringUtils.join(turnovers, ",");

        // 返回结果
        return TurnoverReportVO.builder()
                .dateList(dateList)
                .turnoverList(turnoverList)
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end)
    {
        // 获取每一天的注册人数
        List<User> users = Db.lambdaQuery(User.class)
                .lt(end != null, User::getCreateTime, end.plusDays(1).atStartOfDay())
                .list();
        Map<LocalDate, List<User>> userList = users.stream().collect(Collectors.groupingBy(u -> u.getCreateTime().toLocalDate()));

        // 计算begin之前的总人数
        final LocalDate tmpBegin = begin;
        long total = users.stream().filter(u ->
        {
            return u.getCreateTime().isBefore(tmpBegin.atStartOfDay());
        }).count();

        // 获得范围里的每一天
        List<LocalDate> dates=new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end))
        {
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        // 获得符合格式要求的日期字符串
        String dateList = StringUtils.join(dates, ",");

        // 每天新增人数和总人数
        List<Long> newUsers=new ArrayList<>();
        List<Long> totalUsers=new ArrayList<>();

        // 计算结果
        for (LocalDate date : dates)
        {
            List<User> tmpUsers = userList.get(date);
            if(tmpUsers==null||tmpUsers.isEmpty())
            {
                newUsers.add(0L);
                totalUsers.add(total);
                continue;
            }
            total+=userList.get(date).size();
            newUsers.add((long) userList.get(date).size());
            totalUsers.add(total);
        }

        // 获得符合格式要求的字符串
        String newUserList = StringUtils.join(newUsers, ",");
        String totalUserList = StringUtils.join(totalUsers, ",");

        // 返回结果
        return UserReportVO.builder()
                .dateList(dateList)
                .newUserList(newUserList)
                .totalUserList(totalUserList)
                .build();
    }
}
