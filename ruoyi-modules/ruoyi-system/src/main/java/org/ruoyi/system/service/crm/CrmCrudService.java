package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * CRM 通用 CRUD 服务。
 *
 * @param <T> 实体类型
 * @param <V> 视图对象类型
 * @param <B> 业务对象类型
 */
public abstract class CrmCrudService<T extends TenantEntity, V, B extends BaseEntity> {

    private final BaseMapperPlus<T, V> baseMapper;

    protected CrmCrudService(BaseMapperPlus<T, V> baseMapper) {
        this.baseMapper = baseMapper;
    }

    public TableDataInfo<V> selectPageList(B bo, PageQuery pageQuery) {
        Page<V> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        fillDisplayFields(page.getRecords());
        return TableDataInfo.build(page);
    }

    public List<V> selectList(B bo) {
        List<V> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillDisplayFields(list);
        return list;
    }

    public V selectById(Serializable id) {
        V vo = baseMapper.selectVoById(id);
        fillDisplayFields(vo == null ? List.of() : List.of(vo));
        return vo;
    }

    public boolean insert(B bo) {
        T entity = MapstructUtils.convert(bo, getEntityClass());
        return baseMapper.insert(entity) > 0;
    }

    public boolean update(B bo) {
        T entity = MapstructUtils.convert(bo, getEntityClass());
        return baseMapper.updateById(entity) > 0;
    }

    public boolean deleteByIds(Collection<? extends Serializable> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    protected abstract Wrapper<T> buildQueryWrapper(B bo);

    protected abstract Class<T> getEntityClass();

    protected void fillDisplayFields(List<V> records) {
        // 子类按业务显式补齐列表展示字段。
    }
}
