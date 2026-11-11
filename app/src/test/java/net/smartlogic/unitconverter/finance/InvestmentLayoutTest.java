package net.smartlogic.unitconverter.calculator.finance.investment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.widget.SwitchCompat;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.calculator.finance.emi.EmiLayout;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.theme.*;
import org.junit.Test;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Native previews of real fragments, not hand-reconstructed screenshots. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class InvestmentLayoutTest extends InvestmentFragmentTest {
    @Test public void validationMessagesDoNotMoveEditingControls() throws Exception {
        var controller=Robolectric.buildActivity(Host.class).setup().visible();
        try {
            var f=launch(controller.get(),InvestmentInputs.Kind.SIP);
            set(f,R.id.emi_principal,"500");set(f,R.id.emi_rate,"12");set(f,R.id.emi_tenure,"10");drain(f);
            form(f).findViewById(R.id.emi_principal).performClick();
            for(var theme:CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class,"current",theme);
                for(int[] pane:new int[][]{{360,480},{412,612},{800,352}}) {
                    RuntimeEnvironment.setFontScale(pane[0]==412?1.3f:1f);
                    ThemeApplier.apply(f.requireView());
                    measureEditingPane(f,pane);
                    int[] ready=editingBounds(f);
                    assertEquals(View.INVISIBLE,form(f).findViewById(R.id.emi_message).getVisibility());
                    set(f,R.id.emi_tenure,"0");measureEditingPane(f,pane);
                    assertArrayEquals(theme.id()+" pending",ready,editingBounds(f));
                    drain(f);measureEditingPane(f,pane);
                    assertEquals(f.getString(R.string.investment_check_short),((android.widget.TextView)form(f).findViewById(R.id.emi_message)).getText().toString());
                    assertArrayEquals(theme.id()+" invalid",ready,editingBounds(f));
                    set(f,R.id.emi_tenure,"10");drain(f);measureEditingPane(f,pane);
                    assertArrayEquals(theme.id()+" recovered",ready,editingBounds(f));
                    set(f,R.id.emi_principal,"");measureEditingPane(f,pane);
                    assertArrayEquals(theme.id()+" incomplete",ready,editingBounds(f));
                    set(f,R.id.emi_principal,"500");drain(f);measureEditingPane(f,pane);
                }
            }
        } finally {RuntimeEnvironment.setFontScale(1f);controller.pause().stop().destroy();}
    }
    private void measureEditingPane(InvestmentCalculatorFragment f,int[] pane) {
        for(int i=0;i<2;i++) {
            form(f).forceLayout();form(f).measure(View.MeasureSpec.makeMeasureSpec(pane[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(pane[1],View.MeasureSpec.EXACTLY));
            form(f).layout(0,0,pane[0],pane[1]);
        }
        assertTrue(((EmiLayout)form(f).findViewById(R.id.emi_layout)).fitsViewport());
    }
    private int[] editingBounds(InvestmentCalculatorFragment f) {
        int[] ids={R.id.emi_fields,R.id.emi_principal,R.id.emi_rate,R.id.emi_tenure,R.id.investment_step_value,R.id.emi_message,R.id.emi_keys,R.id.seven,R.id.equal};
        int[] bounds=new int[ids.length*4];
        for(int i=0;i<ids.length;i++) {
            View view=form(f).findViewById(ids[i]);android.graphics.Rect rect=new android.graphics.Rect();
            view.getDrawingRect(rect);((android.view.ViewGroup)form(f)).offsetDescendantRectToMyCoords(view,rect);
            bounds[i*4]=rect.left;bounds[i*4+1]=rect.top;bounds[i*4+2]=rect.right;bounds[i*4+3]=rect.bottom;
        }
        return bounds;
    }
    @Test public void sipScheduleUsesCompactRowsAndMeasuredPagination() throws Exception {
        RuntimeEnvironment.setFontScale(1f);
        var controller = Robolectric.buildActivity(Host.class).setup().visible();
        try {
            var f = launch(controller.get(), InvestmentInputs.Kind.SIP);
            Preferences.getInstance(controller.get()).getPreferences().edit().putString(Appearance.FONT,Appearance.Font.OSWALD.name()).apply();
            set(f,R.id.emi_principal,"500"); set(f,R.id.emi_rate,"12"); set(f,R.id.emi_tenure,"10"); drain(f);
            for (var theme : new CalculatorTheme[]{CalculatorThemes.PORCELAIN_WHITE, CalculatorThemes.GRAPHITE_BLACK}) {
                ReflectionHelpers.setStaticField(ThemeManager.class,"current",theme);
                ReflectionHelpers.callInstanceMethod(f,"renderSchedule");
                View schedule = ReflectionHelpers.getField(f,"schedule");
                for (int i=0;i<3;i++) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle();
                    schedule.forceLayout();
                    schedule.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
                    schedule.layout(0,0,360,480);
                }
                net.smartlogic.unitconverter.widget.FixedTableView table = ReflectionHelpers.getField(f,"table");
                assertTrue(table.fitsViewport()); assertTrue("At least six monthly rows in " + table.getWidth()+"x"+table.getHeight()+", actual="+(table.end()-table.start()),table.end()-table.start()>=6);
                android.view.ViewGroup row=(android.view.ViewGroup)table.getChildAt(1);
                assertTrue(row.getHeight()>=48);
                assertEquals(schedule.getResources().getDimension(R.dimen.finance_schedule_text_size),((android.widget.TextView)row.getChildAt(0)).getTextSize(),.01f);
                int end=table.end();table.next();schedule.requestLayout();
                schedule.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));schedule.layout(0,0,360,480);
                assertEquals(end,table.start());
                Bitmap bitmap=Bitmap.createBitmap(360,480,Bitmap.Config.ARGB_8888);schedule.draw(new Canvas(bitmap));
                File file=new File("build/reports/investments/SIP-schedule-"+theme.id()+".png");file.getParentFile().mkdirs();
                try(var stream=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
            }
        } finally { controller.pause().stop().destroy(); }
    }
    @Test public void sipLoadingKeepsResultAndControlBoundsStable() throws Exception {
        var controller = Robolectric.buildActivity(Host.class).setup().visible();
        try {
            var f = launch(controller.get(), InvestmentInputs.Kind.SIP);
            set(f, R.id.emi_principal, "500");
            set(f, R.id.emi_rate, "12");
            set(f, R.id.emi_tenure, "10");
            drain(f);
            for (var theme : CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class, "current", theme);
                for (float scale : new float[]{1f, 1.3f}) {
                    for (int[] viewport : new int[][]{{360, 528}, {412, 660}, {800, 400}}) {
                        render(f, theme.id(), scale, viewport[0], viewport[1], false, true, "before-loading");
                        int[] ready = resultBounds(f);
                        // An option change recalculates with the keypad closed.
                        ReflectionHelpers.callInstanceMethod(f, "changed");
                        assertNull(ReflectionHelpers.getField(f, "result"));
                        assertEquals(f.getString(R.string.emi_loading),
                                ((android.widget.TextView) form(f).findViewById(R.id.emi_message)).getText().toString());
                        render(f, theme.id(), scale, viewport[0], viewport[1], false, true, "loading");
                        assertArrayEquals(theme.id() + " pending " + scale, ready, resultBounds(f));
                        assertFalse(form(f).findViewById(R.id.emi_more).isEnabled());
                        drain(f);
                        render(f, theme.id(), scale, viewport[0], viewport[1], false, true, "after-loading");
                        assertArrayEquals(theme.id() + " completed " + scale, ready, resultBounds(f));
                    }
                }
            }
            set(f, R.id.emi_rate, "");
            drain(f);
            assertEquals(View.GONE, form(f).findViewById(R.id.emi_summary).getVisibility());
        } finally {
            RuntimeEnvironment.setFontScale(1f);
            controller.pause().stop().destroy();
        }
    }
    private int[] resultBounds(InvestmentCalculatorFragment f) {
        int[] ids = {R.id.emi_fields, R.id.emi_result_section, R.id.emi_summary, R.id.emi_message, R.id.emi_actions};
        int[] bounds = new int[ids.length * 4];
        for (int i = 0; i < ids.length; i++) {
            View view = form(f).findViewById(ids[i]);
            assertEquals(View.VISIBLE, view.getVisibility());
            bounds[i * 4] = view.getLeft(); bounds[i * 4 + 1] = view.getTop();
            bounds[i * 4 + 2] = view.getRight(); bounds[i * 4 + 3] = view.getBottom();
        }
        return bounds;
    }
    @Test public void financeNativePreviewsAllThemesAndScaledLayouts()throws Exception{
        for(var kind:InvestmentInputs.Kind.values()) {
            var controller=Robolectric.buildActivity(Host.class).setup().visible();
            try {
                var f=launch(controller.get(),kind);
                Preferences.getInstance(controller.get()).getPreferences().edit().putString(Appearance.FONT,Appearance.Font.OSWALD.name()).apply();
                set(f,R.id.emi_principal,kind==InvestmentInputs.Kind.FD?"10000":kind==InvestmentInputs.Kind.RD?"200":"500");
                set(f,R.id.emi_rate,kind==InvestmentInputs.Kind.SIP?"12":"6");set(f,R.id.emi_tenure,kind==InvestmentInputs.Kind.FD?"3":kind==InvestmentInputs.Kind.RD?"5":"10");drain(f);
                for(var theme:CalculatorThemes.all()) {
                    ReflectionHelpers.setStaticField(ThemeManager.class,"current",theme);
                    ThemeApplier.apply(f.requireView());f.requireView().setBackgroundColor(theme.background());
                    render(f,theme.id(),1f,360,600,true,true,"editing");
                    render(f,theme.id()+"-device",1f,360,528,true,true,"editing");
                    if(theme.id().equals(CalculatorThemes.PORCELAIN_WHITE.id())||theme.id().equals(CalculatorThemes.GRAPHITE_BLACK.id()))
                        render(f,theme.id(),1f,360,600,false,true,"results");
                }
                ReflectionHelpers.setStaticField(ThemeManager.class,"current",CalculatorThemes.PORCELAIN_WHITE);
                for(var font:Appearance.Font.values()) {
                    Preferences.getInstance(controller.get()).getPreferences().edit().putString(Appearance.FONT,font.name()).apply();
                    render(f,"scaled-"+font.name(),1.3f,412,660,true,true,"editing");
                }
                Preferences.getInstance(controller.get()).getPreferences().edit().putString(Appearance.FONT,Appearance.Font.OSWALD.name()).apply();
                render(f,"actual-device-workspace",1f,360,528,true,true,"editing");
                if(kind==InvestmentInputs.Kind.FD) {
                    org.robolectric.util.ReflectionHelpers.callInstanceMethod(f,"choice",org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,R.string.investment_periodic));drain(f);
                    render(f,"periodic-device",1f,360,528,true,true,"editing");
                    org.robolectric.util.ReflectionHelpers.callInstanceMethod(f,"choice",org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,R.string.investment_cumulative));drain(f);
                }
                render(f,"landscape",1f,800,400,true,true,"editing");
                render(f,"landscape",1f,800,400,false,true,"results");
                render(f,"extreme",2f,320,300,true,false,"space-limit");
                RuntimeEnvironment.setFontScale(1f);
                if(kind==InvestmentInputs.Kind.SIP){
                    ((SwitchCompat)form(f).findViewById(R.id.investment_step_switch)).setChecked(true);set(f,R.id.investment_step_value,"10");drain(f);
                    render(f,"step-percent",1f,360,600,true,true,"editing");
                    render(f,"step-percent-device",1f,360,528,true,true,"editing");
                }
                set(f,R.id.emi_rate,"");drain(f);render(f,"incomplete-device",1f,360,528,true,true,"validation");
            }finally{RuntimeEnvironment.setFontScale(1f);controller.pause().stop().destroy();}
        }
    }
    @Test public void stepSegmentsUseThemeAndDisabledStates() throws Exception {
        var controller=Robolectric.buildActivity(Host.class).setup().visible();
        try {
            var f=launch(controller.get(),InvestmentInputs.Kind.SIP);
            var selected=(com.google.android.material.button.MaterialButton)form(f).findViewById(R.id.investment_percent_mode);
            var frequency=(com.google.android.material.button.MaterialButton)form(f).findViewById(R.id.investment_step_frequency);
            var toggle=(SwitchCompat)form(f).findViewById(R.id.investment_step_switch);
            for(var theme:CalculatorThemes.all()) {
                ReflectionHelpers.setStaticField(ThemeManager.class,"current",theme);
                ThemeApplier.apply(f.requireView());
                toggle.setChecked(false);
                int disabled=selected.getBackgroundTintList().getColorForState(selected.getDrawableState(),0);
                assertNotEquals(theme.id(),theme.accentContentColor(),disabled);
                assertTrue(theme.id(),androidx.core.graphics.ColorUtils.calculateContrast(selected.getCurrentTextColor(),disabled)>=4.5);
                assertEquals(frequency.getCurrentTextColor(),frequency.getIconTint().getColorForState(frequency.getDrawableState(),0));
                toggle.setChecked(true);
                assertEquals(theme.id(),theme.accentContentColor(),selected.getBackgroundTintList().getColorForState(selected.getDrawableState(),0));
                assertEquals(theme.id(),theme.onAccentColor(),selected.getCurrentTextColor());
            }
        } finally {controller.pause().stop().destroy();}
    }
    private void assertControls(View view,String name) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view.isEnabled()&&(view instanceof android.widget.EditText||view instanceof com.google.android.material.button.MaterialButton||view instanceof SwitchCompat)) {
            assertTrue(name+" control "+view.getId()+" width "+view.getMeasuredWidth(),view.getMeasuredWidth()>=48);
            assertTrue(name+" control "+view.getId()+" height "+view.getMeasuredHeight(),view.getMeasuredHeight()>=48);
            if(view.getParent() instanceof android.view.View parent) assertTrue(name+" control exceeds its parent",view.getLeft()>=0&&view.getRight()<=parent.getWidth());
        }
        if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++)assertControls(group.getChildAt(n),name);
    }
    private void render(InvestmentCalculatorFragment f,String theme,float scale,int width,int height,boolean editing,boolean expected,String state)throws Exception{
        RuntimeEnvironment.setFontScale(scale);ThemeApplier.apply(f.requireView());f.requireView().setBackgroundColor(ThemeManager.get().background());
        if(editing)form(f).findViewById(R.id.emi_principal).performClick();else f.handleBack();
        View root=f.requireView();for(int n=0;n<3;n++){root.forceLayout();root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);root.getViewTreeObserver().dispatchOnPreDraw();Shadows.shadowOf(Looper.getMainLooper()).idle();}
        EmiLayout content=form(f).findViewById(R.id.emi_layout);String name=f.kind()+"-"+theme+"-"+scale+"-"+width+"x"+height+"-"+state;
        assertEquals(name,expected,content.fitsViewport());
        if (expected) {
            android.view.ViewGroup fields = form(f).findViewById(R.id.emi_fields);
            View first = fields.getChildAt(0), second = fields.getChildAt(1);
            if (first.getTop() == second.getTop()) {
                android.graphics.Rect principal = new android.graphics.Rect(), rate = new android.graphics.Rect();
                View principalInput = form(f).findViewById(R.id.emi_principal);
                View rateInput = form(f).findViewById(R.id.emi_rate);
                principalInput.getDrawingRect(principal); rateInput.getDrawingRect(rate);
                fields.offsetDescendantRectToMyCoords(principalInput, principal);
                fields.offsetDescendantRectToMyCoords(rateInput, rate);
                assertEquals(name + " paired numeric editor alignment", principal.top, rate.top);
            }
        }
        View note = form(f).findViewById(R.id.emi_message);
        if (expected && note.getVisibility() == View.VISIBLE && note.getRootView().findViewById(R.id.emi_summary).getVisibility() == View.VISIBLE) {
            assertEquals(R.id.emi_result_section, ((View) note.getParent()).getId());
            assertTrue(name + " note below result card", note.getTop() >= form(f).findViewById(R.id.emi_summary).getBottom());
        }
        if(expected&&editing){View keys=form(f).findViewById(R.id.emi_keys),summary=form(f).findViewById(R.id.emi_result_section);if(width<height&&summary.getVisibility()==View.VISIBLE){
            int margin=root.getResources().getDimensionPixelSize(R.dimen.emi_summary_dense_margin);
            assertTrue(name+" result top space",summary.getTop()-form(f).findViewById(R.id.emi_fields).getBottom()>=margin);
            assertTrue(name+" result bottom space",keys.getTop()-summary.getBottom()>=margin);
        }
            assertTrue(name+" readable result caption",((android.widget.TextView)form(f).findViewById(R.id.emi_value_label)).getTextSize()>=root.getResources().getDimension(R.dimen.graphy_value_text_size));
            assertTrue(name,form(f).findViewById(R.id.seven).getMeasuredHeight()>=48);assertTrue(name,form(f).findViewById(R.id.seven).getMeasuredWidth()>=48);
            assertControls(form(f),name);}
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
        File file=new File("build/reports/investments/"+name+".png");file.getParentFile().mkdirs();try(var stream=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
    }
}
