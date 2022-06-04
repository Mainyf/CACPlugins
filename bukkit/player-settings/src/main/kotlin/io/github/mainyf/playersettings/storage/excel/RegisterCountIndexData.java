package io.github.mainyf.playersettings.storage.excel;

import com.alibaba.excel.annotation.ExcelProperty;

import java.util.Date;
import java.util.Objects;

public class RegisterCountIndexData {

    @ExcelProperty(value = "日期", index = 0)
    private Date date;

    @ExcelProperty(value = "注册数量", index = 1)
    private String registerCount;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getRegisterCount() {
        return registerCount;
    }

    public void setRegisterCount(String registerCount) {
        this.registerCount = registerCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterCountIndexData registerCountIndexData = (RegisterCountIndexData) o;
        return Objects.equals(date, registerCountIndexData.date) && Objects.equals(registerCount, registerCountIndexData.registerCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, registerCount);
    }
}
