package com.rmd.rag;

import java.util.List;

/**
 * Institutional knowledge base for RAG retrieval.
 * Replace FIRM_PRODUCTS and FIRM_POLICIES sections with your firm's actual data before going to production.
 */
public class InstitutionalKnowledgeBase {

    // ── IRS RULES ─────────────────────────────────────────────────────────────

    private static final List<KnowledgeDocument> IRS_RULES = List.of(

        new KnowledgeDocument("rmd-calc", "RMD Calculation Method", "irs-rules",
            "Required Minimum Distribution is calculated by dividing the prior year-end IRA balance " +
            "by the IRS Uniform Lifetime Table life expectancy factor. Key factors by age: " +
            "age 73 = 26.5, age 74 = 25.5, age 75 = 24.6, age 76 = 23.7, age 77 = 22.9, " +
            "age 78 = 22.0, age 79 = 21.1, age 80 = 20.2, age 82 = 18.5, age 85 = 16.0. " +
            "Example: $500,000 balance at age 75 gives RMD of $500,000 / 24.6 = $20,325. " +
            "The RMD deadline is December 31 each year. First-year RMD may be deferred to April 1 of the following year. " +
            "Failure to take the full RMD results in a 25% excise tax on the shortfall amount."),

        new KnowledgeDocument("qcd-rules", "Qualified Charitable Distribution Rules", "irs-rules",
            "A Qualified Charitable Distribution (QCD) allows IRA owners aged 70.5 or older to donate " +
            "up to $105,000 per year (2024, indexed annually) directly from an IRA to a qualified charity. " +
            "QCDs are excluded from taxable income entirely and count toward satisfying the annual RMD. " +
            "The distribution must go directly from the IRA custodian to the charity — the client cannot " +
            "receive the funds first. Donor-Advised Funds and private foundations do not qualify for QCD. " +
            "QCDs are especially valuable for clients in higher tax brackets or near IRMAA thresholds " +
            "because they reduce adjusted gross income without itemizing deductions. " +
            "The charity must be a 501(c)(3) organization. The client receives a receipt from the charity for records."),

        new KnowledgeDocument("rmd-aggregation", "RMD Aggregation Rule for Multiple IRAs", "irs-rules",
            "When a client holds multiple Traditional IRAs, the RMD is calculated separately for each IRA " +
            "based on that account's prior year-end balance. However, the total RMD can be withdrawn from " +
            "any one or any combination of the IRAs — the client is not required to take a proportional " +
            "withdrawal from each account. This aggregation rule applies to Traditional IRAs and SEP-IRAs. " +
            "It does NOT apply to 401(k) or 403(b) accounts — those must each satisfy their own RMD independently. " +
            "Roth IRAs are exempt from RMD requirements during the owner's lifetime under current law. " +
            "Strategic account selection for satisfying the aggregate RMD allows for tax-loss harvesting " +
            "and preserving better-performing assets in the most favorable accounts."),

        new KnowledgeDocument("qlac-rules", "QLAC — Qualifying Longevity Annuity Contract", "irs-rules",
            "A Qualifying Longevity Annuity Contract (QLAC) allows IRA owners to use up to $200,000 " +
            "(or 25% of the IRA balance, whichever is less) to purchase a deferred income annuity " +
            "that begins payments at a future date, typically age 80 or 85. " +
            "The QLAC balance is excluded from the RMD calculation base, reducing current RMD obligations. " +
            "This is a powerful longevity hedge — the client receives guaranteed income in advanced age " +
            "when other assets may be depleted. QLAC payments are taxed as ordinary income when received. " +
            "If the client dies before annuity start date, a death benefit may return premiums to heirs. " +
            "QLACs must meet IRS requirements and cannot have cash surrender value."),

        new KnowledgeDocument("roth-conversion-tax", "Roth Conversion Tax Treatment", "irs-rules",
            "A Roth IRA conversion moves pre-tax Traditional IRA assets to a Roth IRA. " +
            "The converted amount is added to ordinary income in the year of conversion and taxed accordingly. " +
            "There is no income limit for conversions (the backdoor Roth strategy remains available). " +
            "Converted assets grow tax-free and Roth IRAs have no RMD requirement during the owner's lifetime. " +
            "A five-year holding period applies to each conversion for penalty-free withdrawal of converted principal " +
            "before age 59.5. Roth conversions are most advantageous when the client is in a lower tax bracket " +
            "than expected in the future, when they have outside funds to pay the tax bill, " +
            "or when they want to reduce future RMDs and leave a tax-free inheritance to heirs."),

        new KnowledgeDocument("penalty-early", "Early and Missed Distribution Penalties", "irs-rules",
            "Withdrawals from a Traditional IRA before age 59.5 are subject to a 10% early withdrawal penalty " +
            "in addition to ordinary income tax. Exceptions include death, disability, substantially equal " +
            "periodic payments (SEPP/72(t)), first-home purchase up to $10,000, qualified higher education expenses, " +
            "and health insurance premiums during unemployment. " +
            "For missed RMDs, the IRS imposes a 25% excise tax on the amount that was not withdrawn, " +
            "reduced to 10% if corrected within two years. The IRS offers a correction program for inadvertent failures. " +
            "Excess contributions to an IRA are subject to a 6% penalty per year until corrected."),

        new KnowledgeDocument("secure-act", "SECURE Act 2.0 Key Changes for RMDs", "irs-rules",
            "The SECURE Act 2.0 (signed December 2022) made several significant changes to RMD rules. " +
            "The required beginning date was raised from age 72 to age 73 effective January 1, 2023, " +
            "and will rise to age 75 effective January 1, 2033. " +
            "The excise tax for missed RMDs was reduced from 50% to 25%, and to 10% if self-corrected within two years. " +
            "Roth 401(k) accounts are now exempt from RMDs during the owner's lifetime (effective 2024), " +
            "aligning them with Roth IRAs. " +
            "Surviving spouse beneficiaries now have additional options: they may elect to be treated as the deceased spouse " +
            "and delay RMDs until the deceased would have reached the applicable age. " +
            "These changes require revisiting existing RMD plans for clients who turned 72 between 2020-2022.")
    );

    // ── DISTRIBUTION STRATEGIES ────────────────────────────────────────────────

    private static final List<KnowledgeDocument> STRATEGIES = List.of(

        new KnowledgeDocument("strategy-cash", "Cash Distribution Strategy", "strategies",
            "Cash distribution is the most common RMD method. Selected assets are liquidated and the proceeds " +
            "are transferred to the client's bank account or taxable brokerage. " +
            "The full distributed amount is taxable as ordinary income in the year received. " +
            "Best suited for clients who need the funds for living expenses, have low tax brackets, " +
            "or want to rebalance their portfolio while satisfying the RMD. " +
            "Asset selection should prioritize positions with losses or minimal gains to reduce tax impact. " +
            "Highly concentrated positions and long-term appreciated holdings should be preserved when possible. " +
            "The distributed cash can be directed to a money market fund, savings account, or reinvested " +
            "in a taxable account outside the IRA for continued growth."),

        new KnowledgeDocument("strategy-inkind", "In-Kind Transfer Strategy", "strategies",
            "An in-kind distribution transfers actual securities (stocks, ETFs, bonds) from the IRA " +
            "to a taxable brokerage account without selling them. The fair market value on the transfer date " +
            "is treated as a taxable distribution and satisfies the RMD requirement. " +
            "The client retains full market exposure and avoids the risk of selling at a bad time. " +
            "The securities receive a cost basis equal to the fair market value at time of transfer — " +
            "any future appreciation in the taxable account will be taxed at lower capital gains rates. " +
            "In-kind transfers are ideal during volatile markets, for appreciated positions the client wants to hold, " +
            "or when the client has sufficient outside income and does not need the cash. " +
            "The receiving brokerage account must be established at the same or compatible custodian."),

        new KnowledgeDocument("strategy-qcd", "QCD Strategy — Charitable Giving to Satisfy RMD", "strategies",
            "Using a QCD to satisfy all or part of the RMD is the most tax-efficient option for charitably inclined clients. " +
            "The QCD amount reduces adjusted gross income dollar-for-dollar, benefiting clients who " +
            "do not itemize deductions (most take the standard deduction) and those near IRMAA Medicare surcharge thresholds. " +
            "A client in the 24% bracket donating $10,000 via QCD saves $2,400 in federal taxes compared to " +
            "taking a cash distribution and donating separately. " +
            "Best candidates: clients aged 70.5+ with charitable intent, clients near IRMAA thresholds, " +
            "clients in high tax brackets with modest charitable budgets. " +
            "The QCD must go directly to the charity. Only Traditional IRA assets qualify, not 401(k)."),

        new KnowledgeDocument("strategy-roth", "Roth Conversion as RMD Reduction Strategy", "strategies",
            "Converting Traditional IRA assets to Roth reduces the future RMD base, lowering required distributions " +
            "in subsequent years. Conversions are most effective when done in lower-income years (early retirement, " +
            "before Social Security begins, or before RMDs themselves push the client into a higher bracket). " +
            "A systematic partial conversion strategy — converting enough each year to fill the current bracket — " +
            "can eliminate most future RMDs over a 10-15 year horizon. " +
            "Important: a Roth conversion cannot itself satisfy the current year RMD — the RMD must be " +
            "taken first before any remaining balance is converted. " +
            "Tax bill from conversion should ideally be paid from outside (non-IRA) funds to maximize compounding."),

        new KnowledgeDocument("strategy-taxloss", "Tax-Loss Harvesting in the RMD Context", "strategies",
            "Tax-loss harvesting involves selling securities with unrealized losses to offset realized gains elsewhere. " +
            "In the RMD context, assets with embedded losses in the IRA are prioritized for distribution — " +
            "they reduce the taxable basis of what remains. Outside the IRA, losses in the taxable account " +
            "can offset the ordinary income from the RMD using capital loss carryforwards. " +
            "The wash-sale rule prohibits repurchasing the same or substantially identical security within 30 days. " +
            "This strategy is most effective when the client has significant taxable account losses to deploy. " +
            "Asset ranking for RMD selection should weight positions with losses or minimal unrealized gains " +
            "higher priority for liquidation to minimize the overall tax cost of the distribution."),

        new KnowledgeDocument("strategy-ss-coord", "Social Security and RMD Coordination Strategy", "strategies",
            "Coordinating Social Security benefit timing with RMD planning significantly affects lifetime tax burden. " +
            "Clients who delay Social Security to age 70 receive 8% per year credits, maximizing lifetime benefit. " +
            "The gap years between retirement and age 70 (before Social Security and before RMDs begin) " +
            "create an ideal window for Roth conversions at lower tax rates — income may be near zero or very low. " +
            "Once both Social Security and RMDs are active, up to 85% of Social Security may be taxable, " +
            "and the combined income can push clients into higher brackets and IRMAA surcharge tiers. " +
            "Recommended approach: model two scenarios — (a) take SS at 62/66, (b) delay to 70 with Roth conversions. " +
            "Break-even on delayed SS is typically age 81-83; clients in good health strongly benefit from delay."),

        new KnowledgeDocument("strategy-beneficiary", "Beneficiary Designation and Estate Planning for IRA Assets", "strategies",
            "Beneficiary designations on IRAs supersede the will — keeping them current is critical. " +
            "Spouse beneficiaries may roll the inherited IRA into their own IRA, deferring RMDs until their age 73. " +
            "Non-spouse beneficiaries under the SECURE Act must fully distribute inherited IRAs within 10 years " +
            "(the 10-year rule), with exceptions for eligible designated beneficiaries (minor children, disabled, " +
            "not more than 10 years younger than the deceased, and surviving spouses). " +
            "Naming a trust as beneficiary provides control but may accelerate distributions — " +
            "only conduit and accumulation trusts meeting specific IRS requirements qualify for stretch treatment. " +
            "Roth IRA inheritance: no income tax on distributions but the 10-year rule still applies. " +
            "Recommendation: review beneficiary designations annually and after any major life event.")
    );

    // ── FIRM PRODUCTS ─────────────────────────────────────────────────────────
    // Demo data below — replace with actual firm product names, tickers, and current rates.

    private static final List<KnowledgeDocument> FIRM_PRODUCTS = List.of(

        new KnowledgeDocument("product-mmf", "Apex Prime Money Market Fund (APMXX)", "products",
            "Apex Prime Money Market Fund (ticker: APMXX) is the firm's recommended default landing account " +
            "for cash RMD distributions. Current 7-day yield: 4.92% (as of Q2 2024). Expense ratio: 0.11%. " +
            "The fund invests in US Treasury bills, government agency securities, and high-grade commercial paper " +
            "with a weighted average maturity under 60 days. " +
            "Minimum investment: $1,000. Settlement: same day for IRA-to-taxable transfers before 3:00 PM ET. " +
            "Clients receive a competitive yield while assets remain under management at Apex Wealth. " +
            "Note: money market funds are not FDIC insured but historically maintain a $1.00 NAV. " +
            "Suitable for all risk profiles as a short-term holding vehicle for RMD proceeds."),

        new KnowledgeDocument("product-bond", "Apex Ultra-Short Bond Fund (APUSB)", "products",
            "Apex Ultra-Short Bond Fund (ticker: APUSB) provides higher yield than money market with modest rate risk. " +
            "Current SEC yield: 5.04%. Expense ratio: 0.09%. Average duration: 1.6 years. " +
            "Portfolio composition: 45% US Treasuries, 30% agency mortgage-backed, 25% investment-grade corporate. " +
            "Minimum credit quality: A-rated or better. Maximum single-issuer exposure: 5%. " +
            "Recommended for RMD proceeds the client does not need for 6-18 months. " +
            "Suitable as an in-kind transfer destination from the IRA or as a reinvestment vehicle in the taxable account. " +
            "Price fluctuates slightly with rate changes but recovers within the duration window. " +
            "Outperforms APMXX money market by approximately 12-15 basis points on an annualized basis."),

        new KnowledgeDocument("product-daf", "Apex Charitable Giving Fund (Donor-Advised Fund)", "products",
            "Apex Charitable Giving Fund is the firm's Donor-Advised Fund platform for philanthropically inclined clients. " +
            "Administration fee: 0.50% annually on assets under $250,000; 0.35% above $250,000. " +
            "Minimum contribution: $2,500. Investment options: Conservative Income, Balanced Growth, Aggressive Growth. " +
            "Clients can contribute appreciated securities (taxable account) to the DAF for immediate deduction " +
            "and grant to charities over time. " +
            "Important note: IRA assets cannot be QCDed into a DAF — QCDs must go to operating 501(c)(3) charities. " +
            "However, RMD cash proceeds received by the client CAN be donated to the DAF for a deduction in the same year. " +
            "Apex DAF fee of 0.50% compares favorably to Fidelity Charitable (0.60%) and Schwab Charitable (0.60%). " +
            "Minimum grant to a charity: $250. Online grant request processed within 3-5 business days."),

        new KnowledgeDocument("product-cd", "Apex Certificate of Deposit Ladder", "products",
            "Apex CD Ladder program offers FDIC-insured fixed-rate certificates across multiple terms. " +
            "Current rates (June 2024): 3-month: 5.10%, 6-month: 5.15%, 12-month: 5.20%, 24-month: 4.95%, 36-month: 4.75%. " +
            "FDIC insured up to $250,000 per depositor per institution. " +
            "Early withdrawal penalty: 90 days interest for terms up to 12 months; 180 days for longer terms. " +
            "CD laddering strategy: divide RMD proceeds into 3-4 equal portions across different maturities " +
            "to maintain liquidity while capturing higher rates on longer terms. " +
            "Minimum purchase: $1,000 per CD. Auto-renew at maturity at the prevailing rate (can opt out). " +
            "Recommended allocation: no more than 25% of total RMD proceeds in CDs to preserve flexibility. " +
            "Best for conservative clients who want guaranteed yield with no market risk."),

        new KnowledgeDocument("product-managed", "Apex Managed Income Portfolio", "products",
            "Apex Managed Income Portfolio is a separately managed account (SMA) designed for retirees " +
            "seeking diversified income with capital preservation. Minimum account size: $100,000. " +
            "Annual management fee: 0.75% on the first $500,000; 0.60% above. " +
            "Target allocation: 50% investment-grade bonds, 25% dividend-paying equities, 15% TIPS, 10% cash. " +
            "Current portfolio yield: approximately 4.3% annualized. Maximum equity exposure: 30% of portfolio. " +
            "Quarterly rebalancing. Tax-loss harvesting available for accounts above $250,000. " +
            "Suitable for clients who want professional management of their post-RMD taxable assets. " +
            "In-kind RMD transfers can go directly into this SMA if already enrolled. " +
            "Performance benchmark: 50% Bloomberg US Aggregate / 50% S&P 500 Dividend Aristocrats Index.")
    );

    // ── FIRM POLICIES ─────────────────────────────────────────────────────────

    private static final List<KnowledgeDocument> FIRM_POLICIES = List.of(

        new KnowledgeDocument("policy-cash-default", "Firm Policy — Cash Distribution Default Routing", "policies",
            "Apex Wealth standard policy for cash RMD distributions: direct proceeds to APMXX money market fund " +
            "unless the client has a directed instruction to an external account on file. " +
            "This retains AUM within the firm while providing the client a competitive 4.92% yield. " +
            "If two money market options have client outcomes within 0.20% of each other, prefer the firm product. " +
            "FA must document the routing rationale in the client file within 5 business days of distribution. " +
            "Client preference overrides firm default — honor all written directed distribution instructions. " +
            "For distributions over $50,000, FA should schedule a 15-minute review call with the client " +
            "before processing to confirm intent and review tax implications."),

        new KnowledgeDocument("policy-inkind-routing", "Firm Policy — In-Kind Transfer Destination", "policies",
            "Default policy for in-kind RMD transfers: move securities to the client's Apex Brokerage taxable account. " +
            "If the client does not have an existing Apex taxable account, recommend opening one before the transfer — " +
            "account opening takes 1-2 business days and there is no minimum balance requirement. " +
            "In-kind transfers within Apex (IRA to Apex Brokerage) incur no transfer fee. " +
            "External DTC transfers to outside brokerages incur a $75 outgoing transfer fee per account per year. " +
            "Avoid recommending external transfers unless the client specifically requests it and the documentation " +
            "supports that the external option is clearly in their best interest (Reg BI requirement). " +
            "In-kind transfers retain AUM under management and allow continued portfolio oversight."),

        new KnowledgeDocument("policy-large-dist", "Firm Policy — Large Distribution Review Protocol", "policies",
            "For any single IRA distribution exceeding $100,000, the following steps are required: " +
            "(1) FA completes a Distribution Review Form noting the client's stated purpose and tax situation. " +
            "(2) Branch manager or compliance supervisor must approve before processing. " +
            "(3) Client must provide written distribution instruction (email or DocuSign accepted). " +
            "(4) Withholding notice must be provided: clients may elect 0% federal withholding in writing, " +
            "otherwise the default is 10% mandatory withholding sent to the IRS. " +
            "State withholding requirements vary — confirm the client's state of residence before processing. " +
            "(5) After processing, FA must update the client's financial plan in the CRM system within 10 days. " +
            "Rationale: large distributions have outsized tax and cash-flow impact and require heightened care."),

        new KnowledgeDocument("policy-withholding", "Tax Withholding on IRA Distributions", "policies",
            "IRS requires IRA custodians to withhold 10% federal income tax on distributions unless the client " +
            "elects otherwise in writing using IRS Form W-4P. " +
            "Clients who expect to owe additional tax (e.g., large RMDs pushing them into a higher bracket) " +
            "may elect a higher withholding percentage (20-37%) to avoid underpayment penalties. " +
            "Clients who pay estimated quarterly taxes may prefer to elect 0% withholding and manage payments themselves. " +
            "State withholding: approximately 40 states mandate state income tax withholding on IRA distributions. " +
            "Default withholding rates vary by state — confirm current rate for the client's state of residence. " +
            "Withholding elections can be changed at any time using Form W-4P on file with the custodian. " +
            "Reminder: RMD withholding is treated as paid equally throughout the year even if the RMD is taken in December.")
    );

    // ── COMPLIANCE ────────────────────────────────────────────────────────────

    private static final List<KnowledgeDocument> COMPLIANCE = List.of(

        new KnowledgeDocument("reg-bi", "Regulation Best Interest (Reg BI)", "compliance",
            "SEC Regulation Best Interest (Reg BI) requires broker-dealers to act in the best interest " +
            "of retail customers when making recommendations for securities transactions or investment strategies. " +
            "Four component obligations: (1) Disclosure Obligation — disclose material facts about the recommendation " +
            "and any conflicts of interest; (2) Care Obligation — exercise reasonable diligence to recommend " +
            "what is in the customer's best interest; (3) Conflict of Interest Obligation — establish policies " +
            "to identify and mitigate conflicts; (4) Compliance Obligation — maintain written compliance policies. " +
            "Reg BI applies to securities recommendations. It does not override the client's explicit preference. " +
            "Any recommendation that benefits the firm but not the client must be disclosed and documented. " +
            "Institutional preference may be applied only when the client outcome is neutral or positive."),

        new KnowledgeDocument("suitability", "Suitability Standards and Risk Tolerance", "compliance",
            "All recommendations must be suitable for the specific client based on their: " +
            "investment objectives (income, growth, capital preservation), risk tolerance (conservative, moderate, aggressive), " +
            "time horizon, liquidity needs, tax situation, and overall financial profile. " +
            "For RMD clients, key suitability factors include: age (typically 73+), income needs, " +
            "other income sources (Social Security, pension), existing tax bracket, estate planning goals, " +
            "and health/longevity considerations. " +
            "A QLAC annuity recommendation is only suitable for clients with strong longevity indicators " +
            "and sufficient liquid assets outside the IRA. " +
            "Aggressive growth products are generally unsuitable for the portion of assets designated for RMD funding."),

        new KnowledgeDocument("fee-disclosure", "Fee Disclosure and Transparency Requirements", "compliance",
            "All fees associated with recommended products must be disclosed to the client before the transaction. " +
            "Required disclosures: management fees or expense ratios, transaction fees or commissions, " +
            "surrender charges or early withdrawal penalties, and any 12b-1 fees on mutual funds. " +
            "Form ADV Part 2A must be provided to advisory clients annually and upon material changes. " +
            "For annuity products, the full surrender charge schedule and mortality expense charges must be disclosed. " +
            "Fee disclosure is mandatory even when recommending in-house products — the FA must acknowledge " +
            "awareness of the institutional relationship in the client file. " +
            "Comparative fee disclosure (firm product vs market alternative) is best practice under Reg BI."),

        new KnowledgeDocument("irmaa", "IRMAA — Medicare Premium Surcharge Thresholds", "compliance",
            "IRMAA (Income-Related Monthly Adjustment Amount) increases Medicare Part B and Part D premiums " +
            "for clients whose modified adjusted gross income (MAGI) exceeds certain thresholds. " +
            "2024 thresholds (single filers): below $103,000 = base premium $174.70/mo; " +
            "$103,000-$129,000 = $244.60/mo (+$69.90); $129,000-$161,000 = $349.40/mo (+$174.70); " +
            "$161,000-$193,000 = $454.20/mo (+$279.50); above $193,000 = $559.00/mo (+$384.30). " +
            "For married filing jointly, thresholds are exactly double the single thresholds. " +
            "RMD income can push clients over IRMAA thresholds, significantly increasing healthcare costs. " +
            "Mitigation strategies: QCD reduces MAGI; partial Roth conversions in lower-income years; " +
            "spreading large distributions across multiple tax years if legally possible. " +
            "Always model IRMAA impact before recommending a distribution amount that crosses a threshold boundary."),

        new KnowledgeDocument("fiduciary", "Fiduciary Duty for Investment Advisers", "compliance",
            "Investment Advisers registered under the Investment Advisers Act of 1940 owe clients a fiduciary duty — " +
            "a higher standard than the Reg BI best-interest standard applicable to broker-dealers. " +
            "Fiduciary duty has two components: duty of care and duty of loyalty. " +
            "Duty of care requires providing investment advice in the client's best interest based on their objectives. " +
            "Duty of loyalty requires disclosing and managing conflicts of interest. " +
            "For dually registered representatives (both RIA and broker-dealer), the applicable standard depends on " +
            "which hat is being worn for the specific interaction — this must be clearly communicated to the client. " +
            "RMD recommendations should be documented with the rationale showing how the recommendation serves " +
            "the client's specific financial profile, not just a general best-practice approach."),

        new KnowledgeDocument("do-not-call", "Elderly and Vulnerable Client Protection Rules", "compliance",
            "FINRA Rule 4512 and various state elder financial exploitation laws require heightened protections " +
            "for clients aged 65 and older. " +
            "Required: obtain the name of a trusted contact person for all accounts held by clients 65+. " +
            "Firms may place a temporary hold on a disbursement if financial exploitation of an elderly client " +
            "is reasonably suspected — hold may last up to 15 business days. " +
            "Warning signs of undue influence: sudden changes to beneficiary designations, large unusual transfers, " +
            "a new third party speaking on behalf of the client, urgency or confusion. " +
            "FA must document any observed warning signs and escalate to branch compliance. " +
            "RMD distributions that appear unusually large relative to the client's stated needs should prompt " +
            "a verification call directly to the client before processing.")
    );

    // ── SCENARIOS ─────────────────────────────────────────────────────────────
    // Sample client scenarios for demo and training purposes.

    private static final List<KnowledgeDocument> SCENARIOS = List.of(

        new KnowledgeDocument("scenario-typical", "Typical RMD Client Profile — Age 75 Moderate Risk", "scenarios",
            "Representative profile: client age 75, IRA balance $485,000, spouse age 72. " +
            "RMD calculation: $485,000 / 24.6 (age 75 factor) = $19,715 required distribution. " +
            "Income: Social Security $28,000/yr, small pension $12,000/yr, total pre-RMD MAGI = $40,000. " +
            "With RMD added: MAGI = $59,715 — well below IRMAA threshold of $103,000. " +
            "Tax bracket: 22% marginal on RMD income. " +
            "Recommended strategy: cash distribution to APMXX money market for liquidity. " +
            "Client uses roughly $15,000/yr for living expenses — surplus $5,000 can be reinvested in taxable account. " +
            "Consider partial Roth conversion in same year if bracket has room (22% bracket ceiling is $89,075 single/MFJ adjusted). " +
            "Annual review: confirm beneficiary designation, update account balance for next year RMD calculation."),

        new KnowledgeDocument("scenario-charitable", "Charitable Client Profile — QCD Opportunity", "scenarios",
            "Representative profile: client age 78, IRA balance $920,000, regular charitable donor $12,000/yr. " +
            "RMD calculation: $920,000 / 22.0 (age 78 factor) = $41,818 required distribution. " +
            "Current income: Social Security $34,000, investment income $18,000, MAGI before RMD = $52,000. " +
            "With full cash RMD: MAGI = $93,818 — approaching but below $103,000 IRMAA threshold. " +
            "With QCD of $12,000 and remaining $29,818 as cash: MAGI = $81,818 — safely below IRMAA. " +
            "Tax savings from QCD vs cash donation: client saves $2,880 (24% x $12,000) in federal tax " +
            "plus avoids a potential IRMAA surcharge of ~$835/yr. Total benefit: approximately $3,700 annually. " +
            "Action: instruct custodian to make $12,000 QCD directly to client's designated charity; " +
            "take remaining $29,818 as cash distribution to APMXX money market."),

        new KnowledgeDocument("scenario-inkind", "Volatile Market Client — In-Kind Transfer Scenario", "scenarios",
            "Representative profile: client age 74, IRA balance $1.2M, heavy equity allocation (75% stocks). " +
            "RMD calculation: $1,200,000 / 25.5 (age 74 factor) = $47,059 required distribution. " +
            "Market context: equity markets down 15% YTD — client strongly prefers not to sell at depressed prices. " +
            "Client has ample outside income (pension $45,000, SS $32,000) and does not need RMD cash. " +
            "Recommended strategy: in-kind transfer of $47,059 worth of ETF shares (e.g., 300 shares of VTI) " +
            "to the client's Apex Brokerage taxable account. FMV on transfer date triggers the taxable event. " +
            "Client retains market exposure and participates in any recovery in the taxable account. " +
            "New cost basis in taxable account equals FMV at transfer = depressed price, " +
            "so future appreciation above that basis taxed at favorable long-term capital gains rates. " +
            "FA documents: client preference for in-kind, availability of taxable account, suitability rationale.")
    );

    // ── COMBINED INDEX ─────────────────────────────────────────────────────────

    public static final List<KnowledgeDocument> ALL = combine(
        IRS_RULES, STRATEGIES, FIRM_PRODUCTS, FIRM_POLICIES, COMPLIANCE, SCENARIOS
    );

    @SafeVarargs
    private static List<KnowledgeDocument> combine(List<KnowledgeDocument>... lists) {
        java.util.ArrayList<KnowledgeDocument> combined = new java.util.ArrayList<>();
        for (List<KnowledgeDocument> list : lists) combined.addAll(list);
        return java.util.Collections.unmodifiableList(combined);
    }
}
