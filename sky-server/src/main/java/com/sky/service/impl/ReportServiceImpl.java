package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
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
        List<Double> turnovers = new ArrayList<>();
        dates.stream().forEach(d ->
        {
            List<Orders> orderList = orders.get(d);
            if (orderList == null)
            {
                turnovers.add(0.0);
            }
            if (orderList != null)
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
     *
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
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end))
        {
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        // 获得符合格式要求的日期字符串
        String dateList = StringUtils.join(dates, ",");

        // 每天新增人数和总人数
        List<Long> newUsers = new ArrayList<>();
        List<Long> totalUsers = new ArrayList<>();

        // 计算结果
        for (LocalDate date : dates)
        {
            List<User> tmpUsers = userList.get(date);
            if (tmpUsers == null || tmpUsers.isEmpty())
            {
                newUsers.add(0L);
                totalUsers.add(total);
                continue;
            }
            total += userList.get(date).size();
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

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end)
    {
        // 获得时间区间里的每一天
        List<LocalDate> dates = new ArrayList<>();
        LocalDate tmpBegin = begin;
        dates.add(tmpBegin);
        while (!tmpBegin.equals(end))
        {
            tmpBegin = tmpBegin.plusDays(1);
            dates.add(tmpBegin);
        }

        // 获得符合格式要求的日期集合字符串
        String dateList = StringUtils.join(dates, ",");

        // 查询时间区间内的所有订单
        List<Orders> orders = Db.lambdaQuery(Orders.class)
                .ge(Orders::getCheckoutTime, begin.atStartOfDay())
                .lt(Orders::getCheckoutTime, end.plusDays(1).atStartOfDay())
                .list();

        // 获得时间区间内的所有订单总数
        Integer totalOrderCount = (int) orders.stream().count();

        // 获得符合格式要求的所有订单字符串
        Map<LocalDate, List<Orders>> orderList = orders.stream().collect(Collectors.groupingBy(o -> o.getCheckoutTime().toLocalDate()));
        List<Long> orderCounts = new ArrayList<>();
        for (LocalDate date : dates)
        {
            List<Orders> tmpOrders = orderList.get(date);
            if (tmpOrders == null || tmpOrders.isEmpty())
            {
                orderCounts.add(0L);
                continue;
            }
            orderCounts.add((long) tmpOrders.size());
        }
        String orderCountList = StringUtils.join(orderCounts, ",");

        // 获得时间区间内的有效订单
        List<Orders> validOrders = orders.stream().filter(o ->
        {
            return o.getStatus().equals(Orders.COMPLETED);
        }).toList();

        // 获得时间区间内的有效订单总数
        Integer validOrderCount = (int) validOrders.stream().count();

        // 获得订单完成率
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();

        // 获得符合格式要求的有效订单字符串
        Map<LocalDate, List<Orders>> validOrderList = validOrders.stream().collect(Collectors.groupingBy(o -> o.getCheckoutTime().toLocalDate()));
        List<Long> validOrderCounts = new ArrayList<>();
        for (LocalDate date : dates)
        {
            List<Orders> tmpValidOrders = validOrderList.get(date);
            if (tmpValidOrders == null || tmpValidOrders.isEmpty())
            {
                validOrderCounts.add(0L);
                continue;
            }
            validOrderCounts.add((long) tmpValidOrders.size());
        }
        String validOrderCountList = StringUtils.join(validOrderCounts, ",");

        // 返回结果
        return OrderReportVO.builder()
                .dateList(dateList)
                .orderCountList(orderCountList)
                .validOrderCountList(validOrderCountList)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end)
    {
        // 查询时间区间内的所有订单
        List<Orders> orders = Db.lambdaQuery(Orders.class)
                .ge(Orders::getCheckoutTime, begin.atStartOfDay())
                .lt(Orders::getCheckoutTime, end.plusDays(1).atStartOfDay())
                .eq(Orders::getStatus, Orders.COMPLETED)
                .list();

        // 如果时间区间内订单数为0
        if (orders.isEmpty())
            return SalesTop10ReportVO.builder()
                    .nameList("")
                    .numberList("")
                    .build();

        // 获得时间区间内的所有订单id
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();

        // 查询时间区间内的所有订单细则
        List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class)
                .in(!orderIds.isEmpty(), OrderDetail::getOrderId, orderIds)
                .list();

        // 计算商品与销量的关系
        Map<String, Integer> map = new HashMap<>();
        orderDetails.stream().forEach(o ->
        {
            String name = o.getName();
            Integer number = o.getNumber();
            if (!map.containsKey(name))
                map.put(name, number);
            if (map.containsKey(name))
            {
                Integer tmp = map.get(name);
                number += tmp;
                map.put(name, number);
            }
        });

        // 对销量进行排序并取前十个
        List<Map.Entry<String, Integer>> top10 = map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .toList();

        // 封装结果
        List<String> names = top10.stream().map(Map.Entry::getKey).toList();
        List<Integer> numbers = top10.stream().map(Map.Entry::getValue).toList();
        String nameList = StringUtils.join(names, ",");
        String numberList = StringUtils.join(numbers, ",");

        // 返回结果
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     *
     * @param response
     */
    @Override
    public void export(HttpServletResponse response) throws IOException
    {
        // 获取最近三十天
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        // 获取近三十天新增用户
        Integer newUsers = Db.lambdaQuery(User.class)
                .ge(User::getCreateTime, begin.atStartOfDay())
                .lt(User::getCreateTime,end.plusDays(1).atStartOfDay())
                .count()
                .intValue();

        // 查询近三十日所有订单
        List<Orders> orders = Db.lambdaQuery(Orders.class)
                .ge(Orders::getCheckoutTime, begin.atStartOfDay())
                .lt(Orders::getCheckoutTime,end.plusDays(1).atStartOfDay())
                .list();
        Integer totalOrderCount = orders.size();

        // 获取近三十日有效订单
        List<Orders> validOrders = orders.stream().filter(o -> o.getStatus().equals(Orders.COMPLETED)).toList();
        Integer validOrderCount = validOrders.size();

        // 计算近三十日营业额
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
        BusinessDataVO businessDataVO = BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();

        // 基于模板文件创建一个新的Excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        XSSFWorkbook excel = new XSSFWorkbook(in);

        // 填充数据：时间区间
        XSSFSheet sheet = excel.getSheetAt(0);
        sheet.getRow(1).getCell(1).setCellValue("时间"+begin+"至"+end);

        // 填充数据：营业额
        sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());

        // 填充数据：订单完成率
        sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());

        // 填充数据：新增用户数
        sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());

        // 填充数据：有效订单数
        sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());

        // 填充数据：平均客单价
        sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

        // 通过输出流将Excel文件下载到客户端浏览器
        ServletOutputStream out = response.getOutputStream();
        excel.write(out);

        // 关闭各种流
        in.close();
        out.close();
        excel.close();
    }
}
