package com.smartlearn.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlearn.platform.entity.Banner;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.BannerMapper;
import com.smartlearn.platform.service.BannerService;
import org.springframework.stereotype.Service;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Override
    public Banner createBanner(Banner banner) {
        banner.setIsActive(true);
        save(banner);
        return banner;
    }

    @Override
    public Banner updateBanner(Banner banner) {
        var existing = getById(banner.getId());
        if (existing == null) throw new BizException(404, "轮播图不存在");
        updateById(banner);
        return getById(banner.getId());
    }

    @Override
    public Page<Banner> listBanners(int page, int size) {
        return page(new Page<>(page, size),
            new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder));
    }

    @Override
    public void deleteBanner(Long id) {
        var existing = getById(id);
        if (existing == null) throw new BizException(404, "轮播图不存在");
        removeById(id);
    }

    @Override
    public void toggleActive(Long id) {
        var banner = getById(id);
        if (banner == null) throw new BizException(404, "轮播图不存在");
        banner.setIsActive(!banner.getIsActive());
        updateById(banner);
    }
}
