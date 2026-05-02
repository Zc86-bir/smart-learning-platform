package com.smartlearn.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlearn.platform.entity.OAuth2UserBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OAuth2UserBindingMapper extends BaseMapper<OAuth2UserBinding> {

    @Select("SELECT * FROM oauth2_user_binding WHERE provider = #{provider} AND provider_user_id = #{providerUserId} AND deleted = 0")
    OAuth2UserBinding selectByProviderAndProviderUserId(@Param("provider") String provider,
                                                        @Param("providerUserId") String providerUserId);
}
