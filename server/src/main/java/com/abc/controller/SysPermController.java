package com.abc.controller;

import com.abc.annotation.PermissionRemark;
import com.abc.constant.PermType;
import com.abc.entity.SysPerm;
import com.abc.service.SysPermService;
import com.abc.vo.Json;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * created by CaiBaoHong at 2018/4/17 16:41<br>
 */
@RestController()
@RequestMapping("/sys_perm")
public class SysPermController {

    private static final Logger log = LoggerFactory.getLogger(SysPermController.class);

    @Autowired
    private SysPermService permService;

    @GetMapping("/menu_button_list")
    public Json listMenuButtonPermission(){
        String oper = "list menu and button records";
        EntityWrapper<SysPerm> params = new EntityWrapper<>();
        params.in("ptype", new Integer[]{PermType.MENU,PermType.BUTTON});
        List<SysPerm> list = permService.selectList(params);
        return Json.succ(oper,"list",list);
    }

    /**
     * 新增权限
     *
     * @param body
     * @return
     */
    @PostMapping
    public Json add(@RequestBody String body) {

        String oper = "add permission";
        SysPerm perm = JSON.parseObject(body, SysPerm.class);

        if (StringUtils.isEmpty(perm.getPval())) {
            return Json.fail(oper, "权限值不能为空");
        }

        SysPerm permDB = permService.selectOne(new EntityWrapper<SysPerm>().eq("pval", perm.getPval()));
        if (permDB != null) {
            return Json.fail(oper, "权限值已存在：" + perm.getPval());
        }

        //保存新用户数据
        perm.setCreated(new Date());
        boolean success = permService.insert(perm);
        return Json.result(oper, success)
                .data("pid", perm.getPid())
                .data("created", perm.getCreated());
    }

    @DeleteMapping
    public Json delete(@RequestBody String body) {

        String oper = "delete permission";
        log.info("{}, body: {}", oper, body);

        JSONObject jsonObj = JSON.parseObject(body);
        String pid = jsonObj.getString("pid");

        if (StringUtils.isBlank(pid)) {
            return Json.fail(oper, "无法删除权限：参数为空（权限id）");
        }

        boolean success = permService.deleteById(pid);
        return Json.result(oper, success);
    }

    /**
     * 查询权限
     *
     * @param body
     * @return
     */
    @RequiresPermissions("perm:query")
    @PostMapping("/query")
    public Json query(@RequestBody String body) {

        String oper = "query permission";
        log.info("{}, body: {}", oper, body);

        JSONObject json = JSON.parseObject(body);
        String pname = json.getString("pname");
        String pval = json.getString("pval");

        int current = json.getIntValue("current");
        int size = json.getIntValue("size");
        if (current == 0) current = 1;
        if (size == 0) size = 10;

        Wrapper<SysPerm> queryParams = new EntityWrapper<>();
        queryParams.orderBy("created", false);
        queryParams.orderBy("updated", false);
        if (StringUtils.isNotBlank(pname)) {
            queryParams.like("pname", pname);
        }
        if (StringUtils.isNotBlank(pval)) {
            queryParams.like("pval", pval);
        }
        Page<SysPerm> page = permService.selectPage(new Page<>(current, size), queryParams);
        return Json.succ(oper).data("page", page);
    }

    /**
     * 更新角色
     *
     * @param body
     * @return
     */
    @RequiresPermissions("perm:update")
    @PatchMapping("/info")
    public Json update(@RequestBody String body) {

        String oper = "update permission";
        log.info("{}, body: {}", oper, body);

        SysPerm perm = JSON.parseObject(body, SysPerm.class);
        if (StringUtils.isBlank(perm.getPid())) {
            return Json.fail(oper, "无法更新权限：参数为空（权限id）");
        }
        perm.setUpdated(new Date());
        boolean success = permService.updateById(perm);
        return Json.result(oper, success).data("updated", perm.getUpdated());
    }


    @Autowired
    private ApplicationContext context;

    @GetMapping("/meta/api")
    public Json listApiPermMetadata() {

        String oper = "list api permission metadata";
        log.info(oper);

        Map<String, Object> map = context.getBeansWithAnnotation(PermissionRemark.class);
        Collection<Object> beans = map.values();

        for (Object bean : beans) {
            Class<?> clz = bean.getClass();
            System.out.println("class name: "+clz.getSimpleName());
            Class<?> superclass = clz.getSuperclass();
            System.out.println("superclass : "+superclass.getSimpleName());
            Annotation[] annos = superclass.getAnnotations();
            for (Annotation anno : annos) {
                System.out.println("anno:"+ anno.annotationType().getSimpleName());
                PermissionRemark pm = AnnotationUtils.getAnnotation(anno, PermissionRemark.class);
                if (pm!=null){
                    System.out.println("pm: "+pm.value());
                }

            }

        }



        return Json.succ(oper);
    }

}
