package xyz.foolcat.eve.evehelper.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.foolcat.eve.evehelper.common.base.BaseEntity;

import java.util.Collection;
import java.util.List;

/**
 * 用户信息表
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户信息表")
@Data
@TableName(value = "sys_user")
public class SysUser extends BaseEntity implements UserDetails {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "")
    private Integer id;

    /**
     * 用户名
     */
    @TableField(value = "username")
    @Schema(description = "用户名")
    private String username;

    /**
     * 昵称
     */
    @TableField(value = "nickname")
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 性别：1-男 2-女
     */
    @TableField(value = "gender")
    @Schema(description = "性别：1-男 2-女")
    private Boolean gender;

    /**
     * 密码
     */
    @TableField(value = "`password`")
    @Schema(description = "密码")
    private String password;

    /**
     * 用户头像
     */
    @TableField(value = "avatar")
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 联系方式
     */
    @TableField(value = "mobile")
    @Schema(description = "联系方式")
    private String mobile;

    /**
     * 用户状态：1-正常 0-禁用
     */
    @TableField(value = "`status`")
    @Schema(description = "用户状态：1-正常 0-禁用")
    private Boolean status;

    /**
     * 用户邮箱
     */
    @TableField(value = "email")
    @Schema(description = "用户邮箱")
    private String email;

    /**
     * 逻辑删除标识：0-未删除；1-已删除
     */
    @TableField(value = "deleted")
    @Schema(name="逻辑删除标识：0-未删除；1-已删除")
    private Boolean deleted = true;

    public static final String COL_ID = "id";

    public static final String COL_USERNAME = "username";

    public static final String COL_NICKNAME = "nickname";

    public static final String COL_GENDER = "gender";

    public static final String COL_PASSWORD = "password";

    public static final String COL_AVATAR = "avatar";

    public static final String COL_MOBILE = "mobile";

    public static final String COL_STATUS = "status";

    public static final String COL_EMAIL = "email";

    public static final String COL_DELETED = "deleted";

    public static final String COL_GMT_CREATE = "gmt_create";

    public static final String COL_GMT_MODIFIED = "gmt_modified";
    @TableField(exist = false)
    private List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.deleted;
    }
}