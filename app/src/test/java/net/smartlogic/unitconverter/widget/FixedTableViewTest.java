package net.smartlogic.unitconverter.widget;

import android.app.Application;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=34, qualifiers="mdpi")
public class FixedTableViewTest {
    @Test public void previousReturnsToExactPageBeforeShortFinalPageAndAfterRestore() {
        var host=Robolectric.buildActivity(AppCompatActivity.class);
        host.get().setTheme(R.style.AppTheme);host.setup();ThemeManager.init(host.get());
        try {
            List<List<String>> rows=new ArrayList<>();
            for(int i=0;i<9;i++) rows.add(List.of(Integer.toString(i+1),"100"));
            var table=new FixedTableView(host.get());
            table.setData(List.of("Period","Value"),rows,0,index->{},()->{});
            layout(table);
            int firstEnd=table.end();assertTrue(firstEnd>1);assertTrue(firstEnd<rows.size());
            table.next();layout(table);int secondStart=table.start(),secondEnd=table.end();
            assertEquals(firstEnd,secondStart);
            table.next();layout(table);assertEquals(secondEnd,table.start());assertEquals(rows.size(),table.end());
            assertTrue("Final page is shorter",table.end()-table.start()<secondEnd-secondStart);
            table.previous();layout(table);assertEquals(secondStart,table.start());assertEquals(secondEnd,table.end());
            table.previous();layout(table);assertEquals(0,table.start());assertEquals(firstEnd,table.end());
            // A reconstructed schedule has no in-memory navigation trail.
            table.setData(List.of("Period","Value"),rows,secondEnd,index->{},()->{});layout(table);
            table.previous();layout(table);assertEquals(secondStart,table.start());assertEquals(secondEnd,table.end());
        } finally {host.pause().stop().destroy();}
    }
    private void layout(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY));
        view.layout(0,0,360,240);
    }
}
