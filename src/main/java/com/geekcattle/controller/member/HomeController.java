/*
 * Copyright (c) 2017 <l_iupeiyu@qq.com> All rights reserved.
 */

package com.geekcattle.controller.member;

import com.geekcattle.model.member.Member;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * author geekcattle
 * date 2017/3/14 0014 上午 9:54
 */
@Controller
@RequestMapping("/member")
public class HomeController {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private ConcurrentMap<String, Cache> caches;

    public HomeController() {
        this.caches = new ConcurrentHashMap<String, Cache>();
    }

    /**
     * 
     * @Title: index
     * @Description: 请求显示前台首页
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequestMapping("/index")
    public String index(Model model){
        Subject subject = SecurityUtils.getSubject();
        PrincipalCollection principals = subject.getPrincipals();
        Member member = (Member) principals.getPrimaryPrincipal();
        String account = member.getAccount();
        model.addAttribute("account", account);


        Collection<Cache> values = caches.values();
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
                .append(" with ")
                .append(caches.size())
                .append(" cache(s)): [");
        int i = 0;
        for (Cache cache : values) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(cache.toString());
            i++;
        }
        sb.append("]");

        System.out.println(sb.toString());

        return "member/home";
    }

    /**
     * 
     * @Title: login
     * @Description: 请求显示前台登录页面，若已登录则请求显示前台首页
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(){
        try {
            Boolean isAuth = SecurityUtils.getSubject().isAuthenticated();
            if (isAuth) {
                return "redirect:/member/index";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "member/login";
    }

    /**
     * 
     * @Title: reg
     * @Description: 请求显示注册页面，若已登录则请求显示前台首页
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequestMapping(value = "/reg", method = RequestMethod.GET)
    public String reg(){
        try {
            Boolean isAuth = SecurityUtils.getSubject().isAuthenticated();
            if (isAuth) {
                return "redirect:/member/index";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "member/reg";
    }

}
