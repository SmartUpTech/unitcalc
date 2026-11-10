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
