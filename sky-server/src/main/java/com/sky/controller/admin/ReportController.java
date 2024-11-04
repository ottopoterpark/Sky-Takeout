package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

/**
 * 数据统计接口
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;


    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("营业额统计接口: {}  {}", begin, end);
        TurnoverReportVO turnoverReportVO = reportService.turnoverStatistics(begin, end);
        return Result.success(turnoverReportVO);
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("用户统计接口: {}  {}", begin, end);
        UserReportVO userReportVO = reportService.userStatistics(begin, end);
        return Result.success(userReportVO);
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end)
    {
        log.info("订单统计: {}  {}", begin, end);
        OrderReportVO orderReportVO = reportService.ordersStatistics(begin, end);
        return Result.success(orderReportVO);
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end)
    {
        log.info("查询销量排名top10: {}  {}", begin, end);
        SalesTop10ReportVO salesTop10ReportVO=reportService.top10(begin,end);
        return Result.success(salesTop10ReportVO);
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException
    {
        log.info("导出运营数据报表");
        reportService.export(response);
    }
}
