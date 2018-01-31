/*
 * Copyright (c) 2017 <l_iupeiyu@qq.com> All rights reserved.
 */

package com.geekcattle.controller.member;

import com.geekcattle.core.LoginEnum;
import com.geekcattle.core.shiro.CustomerAuthenticationToken;
import com.geekcattle.model.member.Member;
import com.geekcattle.model.valid.ValidMember;
import com.geekcattle.service.member.MemberService;
import com.geekcattle.util.DateUtil;
import com.geekcattle.util.PasswordUtil;
import com.geekcattle.util.ReturnUtil;
import com.geekcattle.util.UuidUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tk.mybatis.mapper.entity.Example;

import javax.validation.Valid;

/**
 * author geekcattle
 * date 2017/3/9 0009 下午 14:28
 */
@RestController
@RequestMapping("/member")
public class MemberController {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MemberService memberService;

    /**
     * 
     * @Title: doLogin
     * @Description: 前台登录验证
     * @param validMember
     * @param bindingResult
     * @param redirectAttributes
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelMap doLogin(@Valid ValidMember validMember, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return ReturnUtil.Error("用户名或密码为空", null, null);
        }
        String username = validMember.getAccount();
        CustomerAuthenticationToken token = new CustomerAuthenticationToken(validMember.getAccount(), validMember.getPassword(), false);
        token.setLoginType(LoginEnum.CUSTOMER.toString());
        //获取当前的Subject
        Subject currentUser = SecurityUtils.getSubject();
        String loginFailMessage = "登录失败";
        try {
            logger.info("对用户[" + username + "]进行登录验证..验证开始");
            currentUser.login(token);
            logger.info("对用户[" + username + "]进行登录验证..验证通过");
        } catch (UnknownAccountException uae) {
            logger.info("对用户[" + username + "]进行登录验证..验证未通过,未知账户");
            loginFailMessage = "未知账户";
            redirectAttributes.addFlashAttribute("message", loginFailMessage);
        } catch (IncorrectCredentialsException ice) {
            logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误的凭证");
            loginFailMessage = "密码不正确";
            redirectAttributes.addFlashAttribute("message", loginFailMessage);
        } catch (LockedAccountException lae) {
            logger.info("对用户[" + username + "]进行登录验证..验证未通过,账户已锁定");
            loginFailMessage = "账户已锁定";
            redirectAttributes.addFlashAttribute("message", loginFailMessage);
        } catch (ExcessiveAttemptsException eae) {
            logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误次数过多");
            loginFailMessage = "用户名或密码错误次数过多";
            redirectAttributes.addFlashAttribute("message", loginFailMessage);
        } catch (AuthenticationException ae) {
            logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
            loginFailMessage = "用户名或密码不正确";
            ae.printStackTrace();
            redirectAttributes.addFlashAttribute("message", loginFailMessage);
        }
        //验证是否登录成功
        if (currentUser.isAuthenticated()) {
            Session session = currentUser.getSession();
            session.setAttribute("loginType", LoginEnum.CUSTOMER.toString());
            logger.info("前台用户[" + username + "]登录认证通过");
            return ReturnUtil.Success("登录成功");
        } else {
            token.clear();
            return ReturnUtil.Error(loginFailMessage, null, null);
        }
    }

    /**
     * 
     * @Title: doReg
     * @Description: 注册
     * @param member
     * @param bindingResult
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequestMapping(value = "/reg", method = RequestMethod.POST)
    public ModelMap doReg(@Valid Member member, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ReturnUtil.Error("用户名或密码为空", null, null);
        }
        try {
            Example example = new Example(Member.class);
            example.createCriteria().andCondition("account = ", member.getAccount());
            Integer userCount = memberService.getCount(example);
            if (userCount > 0) {
                return ReturnUtil.Error("用户名已存在", null, null);
            }
            if (StringUtils.isEmpty(member.getPassword())) {
                return ReturnUtil.Error("密码不能为空", null, null);
            }
            String Id = UuidUtil.getUUID();
            member.setUid(Id);
            String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
            member.setSalt(salt);
            String password = PasswordUtil.createCustomPwd(member.getPassword(), member.getSalt());
            member.setPassword(password);
            member.setState(1);
            member.setCreatedAt(DateUtil.getCurrentTime());
            member.setUpdatedAt(DateUtil.getCurrentTime());
            memberService.insert(member);
            return ReturnUtil.Success("操作成功", null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    /**
     * 
     * @Title: logout
     * @Description: 前台用户退出登录
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @Deprecated // youbc 此方法没用到，走的shiro的退出filter
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout() {
        try {
            System.out.println("MemberController.logout()");
            SecurityUtils.getSubject().logout();
            System.out.println("您已安全退出");
        }catch (Exception e){
            System.out.println(e);
        }
        return "redirect:/member/login";

    }

}
