package com.sky.controller.user;

import com.sky.constant.DefaultConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿接口
 */
@RestController
@RequestMapping("/user/addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 查询当前用户所有地址信息
     *
     * @return
     */
    @GetMapping("/list")
    public Result<List<AddressBook>> list()
    {
        log.info("查询当前用户所有地址信息");
        Long userId = BaseContext.getCurrentId();
        List<AddressBook> addressBooks = addressBookService.lambdaQuery().eq(AddressBook::getUserId, userId).list();
        return Result.success(addressBooks);
    }

    /**
     * 查询默认地址
     *
     * @return
     */
    @GetMapping("/default")
    public Result<AddressBook> getDefault()
    {
        log.info("查询默认地址");
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookService.lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, DefaultConstant.DEFAULT)
                .one();
        return Result.success(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    public Result save(@RequestBody AddressBook addressBook)
    {
        log.info("新增地址:{}", addressBook);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(DefaultConstant.NOT_DEFAULT);
        addressBookService.save(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     *
     * @param id
     * @return
     */
    @PutMapping("/default")
    @Transactional
    public Result setDefault(@RequestBody AddressBook addressBook)
    {
        log.info("设置默认地址:{}", addressBook);

        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();

        // 将其他地址设为非默认
        addressBookService.lambdaUpdate()
                .eq(AddressBook::getUserId,userId)
                .set(AddressBook::getIsDefault, DefaultConstant.NOT_DEFAULT)
                .update();

        // 设置默认地址
        addressBookService.lambdaUpdate()
                .eq(AddressBook::getUserId,userId)
                .eq(AddressBook::getId,addressBook.getId())
                .set(AddressBook::getIsDefault, DefaultConstant.DEFAULT)
                .update();
        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<AddressBook> one(@PathVariable Long id)
    {
        log.info("根据id查询地址:{}",id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    public Result remove(Integer id)
    {
        log.info("根据id删除地址:{}",id);
        addressBookService.removeById(id);
        return Result.success();
    }

    /**
     * 根据id修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    public Result update(@RequestBody AddressBook addressBook)
    {
        log.info("根据id修改地址:{}",addressBook);
        addressBookService.updateById(addressBook);
        return Result.success();
    }
}
