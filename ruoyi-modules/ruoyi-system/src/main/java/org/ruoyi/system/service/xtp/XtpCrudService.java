package org.ruoyi.system.service.xtp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class XtpCrudService<T extends TenantEntity> {

    protected final BaseMapperPlus<T, T> baseMapper;

    protected XtpCrudService(BaseMapperPlus<T, T> baseMapper) {
        this.baseMapper = baseMapper;
    }

    public TableDataInfo<T> selectPageList(T query, PageQuery pageQuery) {
        Page<T> page = baseMapper.selectPage(pageQuery.build(), buildQueryWrapper(query));
        fillDisplayFields(page.getRecords());
        return TableDataInfo.build(page);
    }

    public List<T> selectList(T query) {
        List<T> list = baseMapper.selectList(buildQueryWrapper(query));
        fillDisplayFields(list);
        return list;
    }

    public T selectById(Serializable id) {
        T entity = baseMapper.selectById(id);
        fillDisplayFields(entity == null ? List.of() : List.of(entity));
        return entity;
    }

    public boolean insert(T entity) {
        return baseMapper.insert(entity) > 0;
    }

    public boolean update(T entity) {
        return baseMapper.updateById(entity) > 0;
    }

    public boolean deleteByIds(Collection<? extends Serializable> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    protected abstract Wrapper<T> buildQueryWrapper(T query);

    protected void fillDisplayFields(List<T> records) {
        // 子类按业务显式补齐列表展示字段。
    }
}
