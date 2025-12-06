package com.kids.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Student.
 */
@Table("student")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("nickname")
    private String nickname;

    @Column("age")
    private Integer age;

    @Column("avatar_style")
    private String avatarStyle;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "students" }, allowSetters = true)
    private Parent parent;

    @Column("parent_id")
    private Long parentId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Student id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return this.nickname;
    }

    public Student nickname(String nickname) {
        this.setNickname(nickname);
        return this;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAge() {
        return this.age;
    }

    public Student age(Integer age) {
        this.setAge(age);
        return this;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAvatarStyle() {
        return this.avatarStyle;
    }

    public Student avatarStyle(String avatarStyle) {
        this.setAvatarStyle(avatarStyle);
        return this;
    }

    public void setAvatarStyle(String avatarStyle) {
        this.avatarStyle = avatarStyle;
    }

    public Parent getParent() {
        return this.parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
        this.parentId = parent != null ? parent.getId() : null;
    }

    public Student parent(Parent parent) {
        this.setParent(parent);
        return this;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long parent) {
        this.parentId = parent;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student)) {
            return false;
        }
        return getId() != null && getId().equals(((Student) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Student{" +
            "id=" + getId() +
            ", nickname='" + getNickname() + "'" +
            ", age=" + getAge() +
            ", avatarStyle='" + getAvatarStyle() + "'" +
            "}";
    }
}
