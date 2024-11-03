package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端工作台接口
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 查询今日运营数据
     *
     * @return
     */
    @GetMapping("/businessData")
    public Result<BusinessDataVO> businessData()
    {
        log.info("查询今日运营数据");
        BusinessDataVO businessDataVO = workspaceService.businessData();
        return Result.success(businessDataVO);
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    @GetMapping("/overviewOrders")
    public Result<OrderOverViewVO> overviewOrders()
    {
        log.info("查询订单管理数据");
        OrderOverViewVO orderOverViewVO = workspaceService.overviewOrders();
        return Result.success(orderOverViewVO);
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    @GetMapping("/overviewDishes")
    public Result<DishOverViewVO> overviewDishes()
    {
        log.info("查询菜品总览");
        DishOverViewVO dishOverViewVO = workspaceService.overviewDishes();
        return Result.success(dishOverViewVO);
    }

    /**
     * 查询套餐总览
     * @return
     */
    @GetMapping("/overviewSetmeals")
    public Result<SetmealOverViewVO> overviewSetmeals()
    {
        log.info("查询套餐总览");
        SetmealOverViewVO setmealOverViewVO=workspaceService.overviewSetmeals();
        return Result.success(setmealOverViewVO);
    }
}
