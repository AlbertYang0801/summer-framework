package com.albert.summer.scan;

import com.albert.summer.annotation.ComponentScan;
import com.albert.summer.annotation.Import;
import com.albert.summer.imported.LocalDateConfiguration;
import com.albert.summer.imported.ZonedDateConfiguration;

/**
 * 扫描@ComponentScan所在pkg下所有的类
 * 如果配置了value=pkg，则扫描指定pkg下所有的类
 * @author yangjunwei
 * @date 2024/7/16
 */
@ComponentScan
//import手动导入扫描不到的类
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {


}
