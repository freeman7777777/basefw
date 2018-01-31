/*
 * Copyright (c) 2017 <l_iupeiyu@qq.com> All rights reserved.
 */

package com.geekcattle.controller.console;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.geekcattle.model.console.Menu;
import com.geekcattle.service.console.MenuService;
import com.geekcattle.service.console.RoleMenuService;
import com.geekcattle.util.DateUtil;
import com.geekcattle.util.ReturnUtil;
import com.geekcattle.util.UuidUtil;
import com.geekcattle.util.console.MenuTreeUtil;

import tk.mybatis.mapper.entity.Example;

/**
 * author geekcattle
 * date 2016/10/21 0021 下午 15:58
 */
@Controller
@RequestMapping("/console/menu")
@RequiresAuthentication
public class MenuController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MenuService menuService;

    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 
     * @Title: index
     * @Description: 获取所有菜单，跳转到菜单管理页面进行展示
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("menu:index")
    @RequestMapping(value = "/index", method = {RequestMethod.GET})
    public String index(Model model) {
        ArrayList<Menu> menuLists = new ArrayList<>();
        List<Menu> Lists = menuService.getChildMenuList(menuLists,"0");
        model.addAttribute("menus", Lists);
        return "console/menu/index";
    }

    @RequiresPermissions("menu:list")
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    @Deprecated // youbc 此方法貌似没用到 2018年1月29日15:24:06
    public ModelMap list() {
        ModelMap map = new ModelMap();
        List<Menu> List = menuService.getMenuAll();
        MenuTreeUtil menuTreeUtil = new MenuTreeUtil(List, null);
        List<Menu> treeGridList = menuTreeUtil.buildTreeGrid();
        map.put("treeList", treeGridList);
        map.put("total", List.size());
        return ReturnUtil.Success("加载成功", map, null);
    }

    /**
     * 
     * @Title: edit
     * @Description: 请求显示添加/修改菜单页面
     * @param menu
     * @param model
     * @return 参数
     * @return String 返回类型
     * @throws
     */
    @RequiresPermissions("menu:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.GET})
    public String edit(Menu menu, Model model) {
        if (StringUtils.isEmpty(menu.getParentId())) { // 添加一级菜单
            menu.setParentId("0");
        }
        if (!StringUtils.isEmpty(menu.getMenuId())) { // 修改
            menu = menuService.getById(menu.getMenuId());
            if (!"null".equals(menu)) {
                menu.setUpdatedAt(DateUtil.getCurrentTime());
            }
        } else { // 添加
            menu.setChildNum(0);
            menu.setListorder(0);
            menu.setMenuType("menu");
            menu.setCreatedAt(DateUtil.getCurrentTime());
            menu.setUpdatedAt(DateUtil.getCurrentTime());
            menu.setMenuLevel(getMenuLevel(menu, 1));
        }
        model.addAttribute("menu", menu);
        return "console/menu/from";
    }

    /**
     * 
     * @Title: getMenuLevel
     * @Description: 获取菜单等级
     * @param menu
     * @param menuLevel
     * @return 参数
     * @return int 返回类型
     * @throws
     */
	private int getMenuLevel(Menu menu, int menuLevel) {
		int level = menuLevel;
		String parentId = menu.getParentId();
		if (!StringUtils.equals("0", parentId)) {
			return getMenuLevel(menuService.getById(parentId), ++level);
		}
		return level;
		
	}

	/**
     * 
     * @Title: save
     * @Description: 保存菜单信息
     * @param menu
     * @param result
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequiresPermissions("menu:save")
    @RequestMapping(value = "/save", method = {RequestMethod.POST})
    @Transactional
    @ResponseBody
    public ModelMap save(@Valid Menu menu, BindingResult result) {
        try {
            if (result.hasErrors()) {
                for (ObjectError er : result.getAllErrors())
                    return ReturnUtil.Error(er.getDefaultMessage(), null, null);
            }
            if (StringUtils.isEmpty(menu.getMenuId())) { // 添加
                String Id = UuidUtil.getUUID();
                menu.setMenuId(Id);
                menuService.insert(menu);
            } else { // 修改
                menuService.save(menu);
            }
            if(!menu.getParentId().equals("0")) { // 若添加的是子菜单
                //更新父类总数
                Example example = new Example(Menu.class);
                example.createCriteria().andCondition("parent_id = ", menu.getParentId());
                Integer parentCount = menuService.getCount(example); // 获取当前菜单总共的子菜单数（包括刚添加的）
                Menu parentMenu = menuService.getById(menu.getParentId()); // 获取父菜单
                parentMenu.setChildNum(parentCount);
                menuService.save(parentMenu); // FIXME 没搞明白为什么要更新，是否可以删掉此行？ youbc 2018年1月29日11:05:30
            }
            return ReturnUtil.Success("操作成功", null, "/console/menu/index");
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("操作失败", null, null);
        }
    }

    /**
     * 
     * @Title: updateOrder
     * @Description: 更改菜单排序
     * @param id
     * @param listorder
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @RequiresPermissions("menu:updateOrder")
    @RequestMapping(value = "/updateOrder", method = {RequestMethod.POST})
    @ResponseBody
    public ModelMap updateOrder(String id, Integer listorder) {
        if (StringUtils.isNotBlank(id)) {
            Menu menu = new Menu();
            menu.setListorder(listorder);
            Example example = new Example(Menu.class);
            example.createCriteria()
                    .andCondition("menu_id = ", id);
            menuService.update(menu, example);
            return ReturnUtil.Success("Success", null, null);
        } else {
            return ReturnUtil.Error("Error", null, null);
        }
    }

    /**
     * 
     * @Title: delete
     * @Description: 删除菜单
     * @param ids
     * @return 参数
     * @return ModelMap 返回类型
     * @throws
     */
    @ResponseBody
    @RequiresPermissions("menu:delete")
    @RequestMapping(value = "/delete", method = {RequestMethod.GET})
    public ModelMap delete(String[] ids) {
        try {
            if ("null".equals(ids) || "".equals(ids)) {
                return ReturnUtil.Error("Error", null, null);
            } else {
                for (String id : ids) {
                    roleMenuService.deleteMenuId(id); // 删除角色-菜单关联
                    menuService.deleteById(id); // TODO 改为关联删除子菜单
                }
                return ReturnUtil.Success("Success", null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnUtil.Error("Error", null, null);
        }
    }


}
