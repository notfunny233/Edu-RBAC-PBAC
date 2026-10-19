package eduadmin.Service;

import eduadmin.pojo.PageResult;
import eduadmin.pojo.Permission;
import eduadmin.pojo.PermissionQueryParam;

import java.util.List;

public interface PermissionService {

    PageResult<Permission> page(PermissionQueryParam permissionQueryParam);//分页条件查询
    void save(Permission permission);
    void del(List<Integer> ids);
    Permission getById(Integer id);
    void update(Permission permission);
    List<Permission> findall();
}
