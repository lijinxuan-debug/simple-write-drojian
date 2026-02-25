package com.example.accounting.utils

import com.example.accounting.R
import com.example.accounting.data.model.AccountGroup
import com.example.accounting.data.model.AccountItem
import com.example.accounting.data.model.CategoryGroup
import com.example.accounting.data.model.CategoryItem

object CategoryAndAccountData {
    // 支出类型
    val expenseCategories = listOf(
        CategoryGroup(
            1, "货品材料", listOf(
                CategoryItem(101, "货品材料", "进货支出", R.drawable.ic_buy),
                CategoryItem(102, "货品材料", "包装耗材", R.drawable.ic_box)
            )
        ),
        CategoryGroup(2, "人工支出", listOf(
            CategoryItem(201, "人工支出", "员工工资", R.drawable.ic_salary),
            CategoryItem(202, "人工支出", "员工提成", R.drawable.ic_bonus),
            CategoryItem(203, "人工支出", "员工福利", R.drawable.ic_welfare),
            CategoryItem(204, "人工支出", "员工社保", R.drawable.ic_insurance)
        )),
        CategoryGroup(3, "运营费用", listOf(
            CategoryItem(301, "运营费用", "房租", R.drawable.ic_rent),
            CategoryItem(302, "运营费用", "水电煤气", R.drawable.ic_water),
            CategoryItem(303, "运营费用", "物业管理费", R.drawable.ic_property),
            CategoryItem(304, "运营费用", "日用饮食", R.drawable.ic_food),
            CategoryItem(305, "运营费用", "办公用品", R.drawable.ic_office),
            CategoryItem(306, "运营费用", "快递运输费", R.drawable.ic_express),
            CategoryItem(307, "运营费用", "通信费", R.drawable.ic_phone),
            CategoryItem(308, "运营费用", "交通费", R.drawable.ic_car),
            CategoryItem(309, "运营费用", "油费", R.drawable.ic_oil),
            CategoryItem(310, "运营费用", "差旅费", R.drawable.ic_travel),
            CategoryItem(311, "运营费用", "招待费", R.drawable.ic_host),
        )),
        CategoryGroup(4, "固定资产", listOf(
            CategoryItem(401, "固定资产", "办公设备", R.drawable.ic_device),
//            CategoryItem(402, "固定资产", "购车费", R.drawable.ic_buy_car),
//            CategoryItem(403, "固定资产", "房产", R.drawable.ic_house)
        )),
//        CategoryGroup(5, "财务费用", listOf(
//            CategoryItem(501, "财务费用", "做账报税", R.drawable.ic_tax_service),
//            CategoryItem(502, "财务费用", "税费", R.drawable.ic_tax),
//            CategoryItem(503, "财务费用", "发票", R.drawable.ic_invoice)
//        )),
//        CategoryGroup(6, "推广费用", listOf(
//            CategoryItem(601, "推广费用", "广告费用", R.drawable.ic_ad_cost),
//            CategoryItem(602, "推广费用", "平台推广", R.drawable.ic_platform),
//            CategoryItem(603, "推广费用", "推广活动", R.drawable.ic_activity),
//            CategoryItem(604, "推广费用", "广告费", R.drawable.ic_ad)
//        )),
//        CategoryGroup(7, "股东支出", listOf(
//            CategoryItem(701, "股东支出", "股东分红", R.drawable.ic_dividend),
//            CategoryItem(702, "股东支出", "股东福利", R.drawable.ic_shareholder_welfare)
//        )),
//        CategoryGroup(8, "其他杂项", listOf(
//            CategoryItem(801, "其他杂项", "烂账损失", R.drawable.ic_bad_debt),
//            CategoryItem(802, "其他杂项", "赔偿罚款", R.drawable.ic_fine),
//            CategoryItem(803, "其他杂项", "其他支出", R.drawable.ic_more_expense)
//        ))
    )

    // 收入类型
    val incomeCategories = listOf(
        CategoryGroup(
            1, "营业收入", listOf(
                CategoryItem(1001, "营业收入", "主营业务收入", R.drawable.ic_category_pos),
                CategoryItem(1002, "营业收入", "租赁所得", R.drawable.ic_category_house),
                CategoryItem(1003, "营业收入", "副业收入", R.drawable.ic_category_gift)
            )
        ),
        CategoryGroup(
            2, "金融投资收入", listOf(
                CategoryItem(2001, "金融投资收入", "退税收入", R.drawable.ic_category_tax),
                CategoryItem(2002, "金融投资收入", "投资收入", R.drawable.ic_category_calendar_bill),
                CategoryItem(2003, "金融投资收入", "利息收入", R.drawable.ic_category_bank_card)
            )
        ),
        CategoryGroup(
            3, "其他收入", listOf(
                CategoryItem(3001, "其他收入", "卖废品", R.drawable.ic_category_truck),
                CategoryItem(3002, "其他收入", "退货退款", R.drawable.ic_category_shopping),
                CategoryItem(3003, "其他收入", "意外来钱", R.drawable.ic_category_gold)
            )
        )
    )

    // 账户分类
    val accountGroups = listOf(
        AccountGroup(
            1, "现金账户", listOf(
                AccountItem(101, "现金账户", "现金账户", R.drawable.ic_cash)
            )
        ),
//        AccountGroup(2, "储蓄账户", listOf(
//            AccountItem(201, "储蓄账户", "银行卡", R.drawable.ic_bank_card)
//        )),
//        AccountGroup(3, "虚拟账户", listOf(
//            AccountItem(301, "虚拟账户", "支付宝", R.drawable.ic_alipay),
//            AccountItem(302, "虚拟账户", "微信钱包", R.drawable.ic_wechat_pay)
//        )),
//        AccountGroup(4, "债权账户", listOf(
//            AccountItem(401, "债权账户", "客户应收款", R.drawable.ic_receivable)
//        )),
//        AccountGroup(5, "信用账户", listOf(
//            AccountItem(501, "信用账户", "信用卡", R.drawable.ic_credit_card)
//        )),
//        AccountGroup(6, "负债账户", listOf(
//            AccountItem(601, "负债账户", "供应商应付款", R.drawable.ic_payable)
//        ))
    )

    // 懒加载建立map
    private val categoryLookupMap by lazy {
        (expenseCategories + incomeCategories).flatMap { it.items }.associateBy { it.id }
    }

    private val accountLookupMap by lazy {
        accountGroups.flatMap { it.items }.associateBy { it.id }
    }

    // 通过map查找对应的
    fun getCategoryById(id: Int): CategoryItem? {
        return categoryLookupMap[id]
    }

    fun getAccountById(id: Int): AccountItem? {
        return accountLookupMap[id]
    }

}