package xyz.foolcat.eve.evehelper.domain.model.entity.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.foolcat.eve.evehelper.shared.kernel.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 *
 * @author Leojan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysUser extends BaseEntity implements Serializable {
    /**
     * 用户ID
     */
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别：1-男 2-女
     */
    private Boolean gender;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态：0-禁用 1-开启
     */
    private Boolean status;

    /**
     * 逻辑删除标识：0-未删除；1-已删除
     */
    private Boolean deleted = true;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
