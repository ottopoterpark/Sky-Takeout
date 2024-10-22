package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService{

    @Autowired
    private WeChatProperties weChatProperties;

    // 微信官方获得微信用户openid的url
    private String loginUrl="https://api.weixin.qq.com/sns/jscode2session";

    public UserServiceImpl(WeChatProperties weChatProperties)
    {
        this.weChatProperties = weChatProperties;
    }

    /**
     * 微信登陆
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO)
    {
        // 调用微信接口服务，获得当前微信用户的openid
        Map<String, String> paramMap=new HashMap<>();                   // 封装请求参数
        paramMap.put("appid",weChatProperties.getAppid());
        paramMap.put("secret",weChatProperties.getSecret());
        paramMap.put("js_code",userLoginDTO.getCode());
        paramMap.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(loginUrl, paramMap);         // 获得响应结果
        JSONObject jsonObject = JSON.parseObject(json);                 // 解析响应结果
        String openid = jsonObject.getString("openid");

        // 判断openid是否为空，如果为空表示登陆失败，抛出业务异常
        if(openid==null)
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);

        // 判断当前微信用户是不是新用户
        User user = lambdaQuery().eq(User::getOpenid, openid).one();

        // 如果是新用户，自动完成注册
        if(user==null)
        {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            save(user);
        }

        // 返回微信用户对象
        return user;
    }
}
