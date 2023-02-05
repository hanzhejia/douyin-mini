package com.douyin.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.douyin.common.dto.LoginUserDTO;
import com.douyin.common.dto.UserDTO;
import com.douyin.common.vo.BaseResponseVO;
import com.douyin.common.vo.UserResponseVO;
import com.douyin.entity.User;
import com.douyin.service.UserService;
import com.douyin.utils.MD5Utils;
import com.douyin.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Author:WJ
 * Date:2023/2/2 22:43
 * Description:<>
 */

@Slf4j
@Service
public class UserManage {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     *
     * @param username
     * @param password
     * @return
     */
    @Transactional
    // TODO: 2023/2/5 WJ @Lock此处建议加username的锁
    public UserResponseVO register(String username, String password) {

        log.info("用户{}进行注册", username);
        // 基础参数校验
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return UserResponseVO.fail("请输入账号和密码");
        }
        // 正则表达式验证邮箱合法性
        String pattern = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
        if (!Pattern.matches(pattern, username)) {
            // return UserResponseVO.fail("请输入正确的邮箱");
        }

        // 验证该邮箱是否已经注册过了
        UserDTO userDTO = userService.getByUsername(username);
        if (userDTO != null) {
            // return UserResponseVO.fail("注册失败，该邮箱已被注册");
        }
        // TODO: 2023/2/5 客户端密码已加密，则不用加密处理，否则先加密加盐处理
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);
        userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setPassword(pwdMD5);
        Long userId = userService.add(userDTO);
        LoginUserDTO loginUserDTO = genLoginUserDTO(username, userId);
        String token = TokenUtils.token(loginUserDTO);
        log.info("用户{}注册成功, token:{}", username, token);
        return UserResponseVO.success(userId, token);

    }

    /**
     * 用户登录
     *
     * @param username
     * @param password
     * @return
     */
    public UserResponseVO login(String username, String password) {
        log.info("用户{}进行登录", username);
        // 基础参数校验
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            // return UserResponseVO.fail("请输入账号和密码");
        }

        // 1.查询是否存在该用户
        UserDTO userDTO = userService.getByUsername(username);
        if (userDTO == null) {
            // return UserResponseVO.fail("登录失败，该用户不存在");
        }
        // TODO: 2023/2/5 客户端密码已加密，则不用加密处理，否则先加密加盐处理
        // 2.验证密码是否正确
        String pwdMD5 = MD5Utils.parseStrToMd5L32(password);
        if (!userDTO.getPassword().equals(pwdMD5)) {
            // return UserResponseVO.fail("登录失败，密码不正确");
        }

        LoginUserDTO loginUserDTO = genLoginUserDTO(username, userDTO.getId());
        String token = TokenUtils.token(loginUserDTO);
        log.info("用户{}登录成功, token:{}", username, token);
        return UserResponseVO.success(userDTO.getId(), token);
    }

    /**
     * 组装用户登录信息
     *
     * @param username
     * @param userId
     * @return
     */
    private LoginUserDTO genLoginUserDTO(String username, Long userId) {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setId(userId);
        loginUserDTO.setUsername(username);
        return loginUserDTO;
    }
}
