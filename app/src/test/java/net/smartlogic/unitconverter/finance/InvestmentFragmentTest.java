package net.smartlogic.unitconverter.calculator.finance.investment;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButtonToggleGroup;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiDialogFragment;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=34,qualifiers="mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class InvestmentFragmentTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state){setTheme(R.style.AppTheme);super.onCreate(state);FrameLayout frame=new FrameLayout(this);frame.setId(12345);setContentView(frame);}
    }
    protected InvestmentCalculatorFragment launch(Host host,InvestmentInputs.Kind kind)throws Exception{
        ThemeManager.init(host);Preferences.getInstance(host).getPreferences().edit().remove(InvestmentCalculatorFragment.toolId(kind)+"_workspace_v1").apply();
        var f=InvestmentCalculatorFragment.create(kind);host.getSupportFragmentManager().beginTransaction().add(12345,f).commitNow();
        View root=host.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(820,View.MeasureSpec.EXACTLY));root.layout(0,0,412,820);
        Shadows.shadowOf(Looper.getMainLooper()).idle();drain(f);return f;
    }
    protected View form(InvestmentCalculatorFragment f){return ReflectionHelpers.getField(f,"form");}
    protected void set(InvestmentCalculatorFragment f,int id,String value){((EditText)form(f).findViewById(id)).setText(value);}
    protected void drain(InvestmentCalculatorFragment f)throws Exception{Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));Future<?> work=ReflectionHelpers.getField(f,"calculation");if(work!=null)work.get(10,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    @Test public void destinationsLiveResultsAndInvalidation()throws Exception{
        for(var kind:InvestmentInputs.Kind.values()){
            var host=Robolectric.buildActivity(Host.class).setup().visible();try{
                var f=launch(host.get(),kind);set(f,R.id.emi_principal,"500");set(f,R.id.emi_rate,"6");set(f,R.id.emi_tenure,"5");drain(f);
                assertNotNull(ReflectionHelpers.getField(f,"result"));
                View field=form(f).findViewById(R.id.emi_principal);
                field.dispatchTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,8,8,0));
                field.dispatchTouchEvent(android.view.MotionEvent.obtain(0,1,android.view.MotionEvent.ACTION_UP,8,8,0));
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(View.VISIBLE,form(f).findViewById(R.id.emi_keys).getVisibility());
                assertEquals(View.GONE,form(f).findViewById(R.id.emi_summary).getVisibility());
                form(f).findViewById(R.id.equal).performClick();
                assertEquals(View.GONE,form(f).findViewById(R.id.emi_keys).getVisibility());
                assertEquals(View.VISIBLE,form(f).findViewById(R.id.emi_summary).getVisibility());
                form(f).findViewById(R.id.emi_more).performClick();
                android.content.Intent chooser=Shadows.shadowOf(host.get()).getNextStartedActivity();
                assertEquals(android.content.Intent.ACTION_CHOOSER,chooser.getAction());
                android.content.Intent share=chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT);
                assertEquals(android.content.Intent.ACTION_SEND,share.getAction());
                assertEquals("text/plain",share.getType());
                assertTrue(share.getStringExtra(android.content.Intent.EXTRA_TEXT).contains(((TextView)form(f).findViewById(R.id.emi_value)).getText()));
                assertEquals(kind==InvestmentInputs.Kind.SIP?View.GONE:View.VISIBLE,form(f).findViewById(R.id.investment_date_row).getVisibility());
                set(f,R.id.emi_rate,"");assertNull(ReflectionHelpers.getField(f,"result"));drain(f);
                assertEquals(View.GONE,form(f).findViewById(R.id.emi_summary).getVisibility());
                assertFalse(form(f).findViewById(R.id.emi_more).isEnabled());
                assertEquals(host.get().getString(R.string.emi_no_result),((TextView)form(f).findViewById(R.id.emi_value)).getText().toString());
                form(f).findViewById(R.id.emi_insights).performClick();
                EmiDialogFragment details=(EmiDialogFragment)f.getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG);
                ((androidx.appcompat.app.AlertDialog)details.requireDialog()).getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                EmiDialogFragment reset=(EmiDialogFragment)f.getChildFragmentManager().findFragmentByTag(EmiDialogFragment.TAG);
                ((androidx.appcompat.app.AlertDialog)reset.requireDialog()).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals("",((EditText)form(f).findViewById(R.id.emi_principal)).getText().toString());
            }finally{host.pause().stop().destroy();}
        }
    }
    @Test public void stepModesGuardsAndHistoryRoundTrip()throws Exception{
        var host=Robolectric.buildActivity(Host.class).setup().visible();try{
            var f=launch(host.get(),InvestmentInputs.Kind.SIP);set(f,R.id.emi_principal,"100");set(f,R.id.emi_rate,"0");set(f,R.id.emi_tenure,"1");
            ((SwitchCompat)form(f).findViewById(R.id.investment_step_switch)).setChecked(true);
            set(f,R.id.investment_step_value,"10");((EditText)form(f).findViewById(R.id.investment_step_value)).getText().replace(0,2,"101");
            assertEquals("10",((EditText)form(f).findViewById(R.id.investment_step_value)).getText().toString());
            var modes=(MaterialButtonToggleGroup)form(f).findViewById(R.id.investment_step_modes);modes.check(R.id.investment_amount_mode);
            set(f,R.id.investment_step_value,"25");((EditText)form(f).findViewById(R.id.investment_step_value)).getText().replace(0,2,"-1");assertEquals("25",((EditText)form(f).findViewById(R.id.investment_step_value)).getText().toString());
            modes.check(R.id.investment_percent_mode);assertEquals("10",((EditText)form(f).findViewById(R.id.investment_step_value)).getText().toString());
            drain(f);InvestmentDraft draft=ReflectionHelpers.getField(f,"draft");String payload=draft.json();
            set(f,R.id.emi_principal,"200");f.restoreFromHistory(payload);drain(f);assertEquals("100",((EditText)form(f).findViewById(R.id.emi_principal)).getText().toString());
            f.restoreFromHistory("{}");assertEquals("100",((EditText)form(f).findViewById(R.id.emi_principal)).getText().toString());
            form(f).findViewById(R.id.emi_principal).performClick();assertTrue(f.handleBack());assertFalse(f.handleBack());
        }finally{host.pause().stop().destroy();}
    }
    @Test public void versionedDraftPreservesPartialValuesAndRejectsWrongTool() {
        InvestmentDraft d=new InvestmentDraft(InvestmentInputs.Kind.SIP,"USD");d.amount="100.";d.stepAmount="50";d.stepPercent="12.5";d.stepMonths=6;
        var restored=InvestmentDraft.read(d.json(),InvestmentInputs.Kind.SIP);assertEquals("100.",restored.amount);assertEquals("50",restored.stepAmount);assertEquals(6,restored.stepMonths);
        assertThrows(IllegalArgumentException.class,()->InvestmentDraft.read(d.json(),InvestmentInputs.Kind.RD));
        d.amount=".25";d.rate="0";d.tenure=".5";
        InvestmentCalculator.validate(d.inputs());assertEquals(6,d.inputs().months());
    }
    @Test public void sipUsesPreferredCurrencyForExistingDraftAndPreferenceReturn() throws Exception {
        var host = Robolectric.buildActivity(Host.class).setup().visible();
        try {
            Preferences preferences = Preferences.getInstance(host.get());
            preferences.setPreferredCurrency("USD");
            var f = launch(host.get(), InvestmentInputs.Kind.SIP);
            assertEquals("$", ((TextView) form(f).findViewById(R.id.investment_amount_mode)).getText().toString());
            set(f,R.id.emi_principal,"500"); set(f,R.id.emi_rate,"12"); set(f,R.id.emi_tenure,"10"); drain(f);
            InvestmentResult dollars = ReflectionHelpers.getField(f,"result");
            InvestmentDraft draft = ReflectionHelpers.getField(f,"draft");
            String savedDollars = draft.json();
            host.pause(); preferences.setPreferredCurrency("INR"); host.resume(); drain(f);
            assertEquals("₹", ((TextView) form(f).findViewById(R.id.investment_amount_mode)).getText().toString());
            assertEquals("500", ((EditText) form(f).findViewById(R.id.emi_principal)).getText().toString());
            InvestmentResult rupees = ReflectionHelpers.getField(f,"result");
            assertEquals(0, dollars.maturity().compareTo(rupees.maturity()));
            assertEquals("INR", rupees.inputs().currency());
            f.restoreFromHistory(savedDollars); drain(f);
            draft = ReflectionHelpers.getField(f,"draft"); assertEquals("INR",draft.currency);
            assertEquals("₹", ((TextView) form(f).findViewById(R.id.investment_amount_mode)).getText().toString());
        } finally { host.pause().stop().destroy(); }
    }
}
