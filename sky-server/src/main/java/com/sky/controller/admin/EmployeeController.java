package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeEditPassword;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO)
    {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = BaseException.class)
    public Result<String> logout()
    {
        log.info("退出");
        return Result.success();
    }

    /**
     * 新增员工
     *
     * @param employeeDTO
     * @return
     */

    @AutoFill(OperationType.INSERT)
    @PostMapping
    public Result save(@RequestBody Employee employee)
    {
        log.info("新增员工:{}", employee);

        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        try
        {
            employeeService.save(employee);
        } catch (Exception e)
        {
            throw new BaseException(MessageConstant.USERNAME_DUPLICATE);
        }

        return Result.success();
    }

    /**
     * 分页查询
     *
     * @param query
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO query)
    {
        log.info("分页查询:{}", query);
        //分页查询条件
        Page<Employee> p = Page.of(query.getPage(), query.getPageSize());
        p = employeeService.lambdaQuery()
                .like(query.getName() != null, Employee::getName, query.getName())
                .page(p);
        //封装查询结果
        PageResult pageResult = new PageResult();
        pageResult.setTotal(p.getTotal());
        pageResult.setRecords(p.getRecords());
        //返回结果
        return Result.success(pageResult);
    }

    /**
     * 修改员工状态
     *
     * @param status
     * @param id
     * @return
     */
    @Transactional
    @PostMapping("/status/{status}")
    public Result changeStatus(@PathVariable Integer status, Long id)
    {
        log.info("修改员工状态:{} {}", status, id);
        employeeService.lambdaUpdate()
                .set(Employee::getStatus, status)
                .eq(Employee::getId, id)
                .update();
        return Result.success();
    }

    /**
     * 根据id查找员工
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Employee> getOneById(@PathVariable Long id)
    {
        log.info("根据id查找员工:{}", id);
        Employee employee = employeeService.lambdaQuery().eq(Employee::getId, id).one();
        return Result.success(employee);
    }

    /**
     * 修改员工信息
     *
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @AutoFill(OperationType.UPDATE)
    public Result update(@RequestBody Employee employee)
    {
        log.info("修改员工信息:{}", employee);
        employeeService.lambdaUpdate()
                .eq(Employee::getId, employee.getId())
                .set(Employee::getIdNumber, employee.getIdNumber())
                .set(Employee::getUsername, employee.getUsername())
                .set(Employee::getName, employee.getName())
                .set(Employee::getSex, employee.getSex())
                .set(Employee::getPhone, employee.getPhone())
                .set(Employee::getUpdateUser,employee.getUpdateUser())
                .set(Employee::getUpdateTime,employee.getUpdateTime())
                .update();
        return Result.success();
    }

    /**
     * 修改密码
     * @param editPassword
     * @return
     */
    @PutMapping("/editPassword")
    public Result editPassword(@RequestBody EmployeeEditPassword editPassword)
    {
        String md5oldPassword = DigestUtils.md5DigestAsHex(editPassword.getOldPassword().getBytes());
        String md5newPassword = DigestUtils.md5DigestAsHex(editPassword.getNewPassword().getBytes());
        boolean result = employeeService.lambdaUpdate()
                .eq(Employee::getId,BaseContext.getCurrentId())
                .eq(Employee::getPassword, md5oldPassword)
                .set(Employee::getPassword, md5newPassword)
                .update();
        if(!result)
            return Result.error(MessageConstant.PASSWORD_EDIT_FAILED);
        return Result.success();
    }
}
