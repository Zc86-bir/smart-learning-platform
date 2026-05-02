package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.entity.Banner;

public interface BannerService {
    Banner createBanner(Banner banner);
    Banner updateBanner(Banner banner);
    Page<Banner> listBanners(int page, int size);
    void deleteBanner(Long id);
    void toggleActive(Long id);
}
