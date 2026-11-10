package net.smartlogic.unitconverter.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class InvestmentCalculatorTest {
    private InvestmentInputs input(InvestmentInputs.Kind kind,String amount,String rate,int months) {
        return new InvestmentInputs(kind,new BigDecimal(amount),new BigDecimal(rate),months,"USD",LocalDate.of(2024,1,31),3,false,false,true,BigDecimal.ZERO,12);
    }
    private InvestmentInputs step(String value,boolean percent,int interval,int months) {
        return new InvestmentInputs(InvestmentInputs.Kind.SIP,new BigDecimal("100"),BigDecimal.ZERO,months,"USD",LocalDate.of(2024,1,31),3,false,true,percent,new BigDecimal(value),interval);
    }
    private void money(String expected,BigDecimal actual) { assertEquals(new BigDecimal(expected),actual); }
    @Test public void sipBeginningOfMonthReference() {
        var r=InvestmentCalculator.calculate(input(InvestmentInputs.Kind.SIP,"500","12",120));
        money("116169.54",r.maturity());money("60000.00",r.invested());money("56169.54",r.interest());reconcile(r);
    }
    @Test public void zeroRateAndNegativeSipReturn() {
        var zero=InvestmentCalculator.calculate(input(InvestmentInputs.Kind.SIP,"100","0",12));money("1200.00",zero.maturity());
        var loss=InvestmentCalculator.calculate(input(InvestmentInputs.Kind.SIP,"100","-12",1));money("99.00",loss.maturity());money("-1.00",loss.interest());reconcile(loss);
    }
    @Test public void amountAndPercentStepStartAfterInterval() {
        for(boolean percentage:new boolean[]{false,true}) {
            var r=InvestmentCalculator.calculate(step("10",percentage,3,6));money("630.00",r.invested());money("100.00",r.periods().get(2).contribution());money("110.00",r.periods().get(3).contribution());reconcile(r);
        }
        money("1800.00",InvestmentCalculator.calculate(step("100",true,6,12)).invested());
        money("1300.00",InvestmentCalculator.calculate(step("100",false,12,13)).invested().subtract(new BigDecimal("100.00")));
    }
    @Test public void stepPercentCompoundsButAmountAdds() {
        money("121.00",InvestmentCalculator.calculate(step("10",true,3,7)).periods().get(6).contribution());
        money("120.00",InvestmentCalculator.calculate(step("10",false,3,7)).periods().get(6).contribution());
    }
    @Test public void recurringQuarterlyReferenceAndDates() {
        var r=InvestmentCalculator.calculate(input(InvestmentInputs.Kind.RD,"200","6",60));
        money("14012.71",r.maturity());money("12000.00",r.invested());money("2012.71",r.interest());
        assertEquals(LocalDate.of(2024,2,29),r.periods().get(1).date());assertEquals(LocalDate.of(2024,3,31),r.periods().get(2).date());reconcile(r);
        // One contribution invested for a full quarter: 100 × 1.015, plus later deposits.
        money("600.00",InvestmentCalculator.calculate(input(InvestmentInputs.Kind.RD,"200","0",3)).maturity());
    }
    @Test public void fixedQuarterlyReferenceAndRemainder() {
        var r=InvestmentCalculator.calculate(input(InvestmentInputs.Kind.FD,"10000","6",36));
        money("11956.18",r.maturity());money("1956.18",r.interest());reconcile(r);
        money("10200.75",InvestmentCalculator.calculate(input(InvestmentInputs.Kind.FD,"10000","6",4)).maturity());
        assertEquals(LocalDate.of(2024,2,29),r.periods().get(0).date());
        assertTrue(r.annualYield().subtract(new BigDecimal("6.1363550625")).abs().compareTo(new BigDecimal("1E-20"))<0);
    }
    @Test public void periodicPayoutsNeverCompoundAndPrincipalReturnsOnce() {
        for(int frequency:new int[]{1,3,6,12}) {
            var base=input(InvestmentInputs.Kind.FD,"10000","6",12);
            var i=new InvestmentInputs(base.kind(),base.amount(),base.annualRate(),12,"USD",base.start(),frequency,true,false,true,BigDecimal.ZERO,12);
            var r=InvestmentCalculator.calculate(i);money("600.00",r.interest());money("10600.00",r.receipts());
            money("0.00",r.periods().get(11).balance());
            BigDecimal payouts=r.periods().stream().map(InvestmentPeriod::payout).reduce(BigDecimal.ZERO,BigDecimal::add);money("10600.00",payouts);
            money(frequency==1?"10050.00":frequency==3?"10150.00":frequency==6?"10300.00":"10600.00",r.maturity());reconcile(r);
        }
    }
    @Test public void partialFinalPayoutAndMinorUnitsReconcile() {
        var r=InvestmentCalculator.calculate(new InvestmentInputs(InvestmentInputs.Kind.FD,new BigDecimal("100.01"),new BigDecimal("7"),4,"USD",LocalDate.of(2024,1,31),3,true,false,true,BigDecimal.ZERO,12));
        money("2.33",r.interest());money("100.59",r.maturity());reconcile(r);
    }
    @Test public void currencyPrecisionAndLimitsAreValidated() {
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.FD,"0","6",12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.FD,"1000000000000","6",12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.FD,"1.001","6",12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.RD,"100","-1",12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.SIP,"100","-100",12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(input(InvestmentInputs.Kind.SIP,"100","6",601)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(step("101",true,3,12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(step("-1",false,3,12)));
        assertThrows(IllegalArgumentException.class,()->InvestmentCalculator.calculate(step("100",true,3,600)));
    }
    @Test public void decimalRootIsAccurateAndBounded() {
        BigDecimal x=InvestmentCalculator.cubeRoot(new BigDecimal("1.015"));
        assertTrue(x.pow(3).subtract(new BigDecimal("1.015")).abs().compareTo(new BigDecimal("1E-31"))<0);
    }
    private void reconcile(InvestmentResult r) {
        BigDecimal interest=r.periods().stream().map(InvestmentPeriod::interest).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal deposits=r.periods().stream().map(InvestmentPeriod::contribution).reduce(BigDecimal.ZERO,BigDecimal::add);
        money(r.interest().toPlainString(),interest);money(r.invested().toPlainString(),deposits);
        money(r.receipts().toPlainString(),r.invested().add(r.interest()));
    }
}
