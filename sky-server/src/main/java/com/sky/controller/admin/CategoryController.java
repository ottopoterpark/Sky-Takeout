package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController("adminCategoryController")
@RequestMapping("/admin/category")
@Slf4j
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分类分页查询
     * @param query
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO query)
    {
        log.info("分类分页查询:{}",query);

        // 避免分页参数进入条件查询
        if(query.getName()!=null||query.getType()!=null)
            query.setPage(1);

        // 条件分页查询
        Page<Category> p = Page.of(query.getPage(), query.getPageSize());
        p.addOrder(OrderItem.asc("sort"));
        p=categoryService.lambdaQuery()
                .like(query.getName()!=null, Category::getName,query.getName())
                .eq(query.getType()!=null, Category::getType,query.getType())
                .page(p);

        // 封装查询结果
        PageResult data = PageResult.builder()
                .total(p.getTotal())
                .records(p.getRecords())
                .build();
        return Result.success(data);
    }

    /**
     * 修改分类
     * @param category
     * @return
     */

    @PutMapping
    @AutoFill(OperationType.UPDATE)
    public Result update(@RequestBody Category category)
    {
        log.info("修改分类:{}",category);
        categoryService.lambdaUpdate()
                .eq(Category::getId,category.getId())
                .set(Category::getName,category.getName())
                .set(Category::getSort,category.getSort())
                .set(Category::getUpdateTime,category.getUpdateTime())
                .set(Category::getUpdateUser,category.getUpdateUser())
                .update();
        return Result.success();
    }

    /**
     * 启用，禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result changeStatus(@PathVariable Integer status,Long id)
    {
        log.info("启用，禁用分类:{} {}",status,id);

        // 查询关联的菜品和套餐
        Set<Integer> dishStatus = Db.lambdaQuery(Dish.class).eq(Dish::getCategoryId, id).list()
                .stream().map(Dish::getStatus).collect(Collectors.toSet());
        Set<Integer> setmealStatus = Db.lambdaQuery(Setmeal.class).eq(Setmeal::getCategoryId, id).list()
                .stream().map(Setmeal::getStatus).collect(Collectors.toSet());

        // 如果关联的菜品或套餐启用中则无法禁用分类
        if(status.equals(StatusConstant.DISABLE)&&(dishStatus.contains(StatusConstant.ENABLE)||setmealStatus.contains(StatusConstant.ENABLE)))
            return Result.error(MessageConstant.CATEGORY_DISABLE_FAILED);

        // 修改状态
        categoryService.lambdaUpdate()
                .eq(Category::getId,id)
                .set(Category::getStatus,status)
                .update();
        return Result.success();
    }

    /**
     * 新增分类
     * @param category
     * @return
     */

    @PostMapping
    @AutoFill(OperationType.INSERT)
    public Result save(@RequestBody Category category)
    {
        log.info("新增分类:{}",category);
        category.setStatus(StatusConstant.DISABLE);

        try
        {
            categoryService.save(category);
        } catch (Exception e)
        {
            throw new BaseException(MessageConstant.CATEGORY_DUPLICATE);
        }

        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type)
    {
        log.info("根据类型查询分类");
        List<Category> list = categoryService.lambdaQuery()
                .eq(Category::getType, type)
                .list();
        return Result.success(list);
    }

    /**
     * 根据id删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @Transactional
    public Result delete(Long id)
    {
        log.info("根据id删除分类:{}",id);

        // 查询分类关联的菜品和套餐
        List<Dish> dishes = Db.lambdaQuery(Dish.class)
                .eq(Dish::getCategoryId, id)
                .list();
        List<Setmeal> setmeals = Db.lambdaQuery(Setmeal.class)
                .eq(Setmeal::getCategoryId, id)
                .list();

        // 如果分类关联了菜品或套餐，则不能删除
        if(!dishes.isEmpty())
            return Result.error(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        if(!setmeals.isEmpty())
            return Result.error(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);

        // 删除分类
        categoryService.remove(new LambdaQueryWrapper<Category>().eq(Category::getId, id));
        return Result.success();
    }
}
