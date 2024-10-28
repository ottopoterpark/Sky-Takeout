package com.sky.controller.user;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.StatusConstant;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户端分类接口
 */
@RestController("userCategoryController")
@RequestMapping("/user/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分类条件查询
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type)
    {
        // 查询所有已启用的分类
        List<Category> categories = categoryService.lambdaQuery()
                .eq(type != null, Category::getType, type)
                .eq(Category::getStatus, StatusConstant.ENABLE)
                .list();

        // 获取这些分类的id
        Set<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toSet());

        // 获取分类下有菜品的分类id
        Set<Long> dishCategoryIds = Db.lambdaQuery(Dish.class)
                .in(Dish::getCategoryId, categoryIds).list()
                .stream().map(Dish::getCategoryId).collect(Collectors.toSet());

        // 获取分类下有套餐的分类id
        Set<Long> setmealCategoryIds = Db.lambdaQuery(Setmeal.class)
                .in(Setmeal::getCategoryId, categoryIds).list()
                .stream().map(Setmeal::getCategoryId).collect(Collectors.toSet());

        // 整合有效的分类id
        Set<Long> validCategoryIds=new HashSet<>();
        validCategoryIds.addAll(dishCategoryIds);
        validCategoryIds.addAll(setmealCategoryIds);

        // 获取有效的分类
        List<Category> validCategories = categories.stream().filter(category -> validCategoryIds.contains(category.getId())).toList();

        // 返回结果
        return Result.success(validCategories);
    }
}
