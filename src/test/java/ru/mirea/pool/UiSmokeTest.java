package ru.mirea.pool;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.pool.presentation.fx.util.Views;
import ru.mirea.pool.presentation.fx.util.FxTasks;
import ru.mirea.pool.presentation.fx.controller.MainController;
import ru.mirea.pool.application.AppContext;
import ru.mirea.pool.infrastructure.database.DatabaseManager;
import javafx.scene.control.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="pool.ui.tests",matches="true")
class UiSmokeTest {
    @BeforeAll static void startFx() throws Exception {
        var ready=new CountDownLatch(1);
        Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});
        assertTrue(ready.await(15,TimeUnit.SECONDS));
    }
    @AfterAll static void stopFx(){Platform.exit();}
    static <T> T fx(Callable<T> action) throws Exception {
        var future=new FutureTask<>(action);Platform.runLater(future);return future.get(20,TimeUnit.SECONDS);
    }
    @Test void passwordVisibilityKeepsInputAndClearsBothViews() throws Exception {
        fx(() -> {
            var view = Views.load("login");
            var masked = (PasswordField) view.root().lookup("#passwordField");
            var visible = (TextField) view.root().lookup("#visiblePasswordField");
            var confirmation = (PasswordField) view.root().lookup("#confirmField");
            var visibleConfirmation = (TextField) view.root().lookup("#visibleConfirmField");
            var toggle = (ToggleButton) view.root().lookup("#passwordToggle");
            assertTrue(masked.isVisible());
            assertFalse(visible.isVisible());
            masked.setText("Sample123");
            confirmation.setText("Sample123");
            toggle.fire();
            assertTrue(visible.isVisible());
            assertFalse(masked.isVisible());
            assertEquals("Скрыть", toggle.getText());
            assertEquals("Sample123", visible.getText());
            assertEquals("Sample123", visibleConfirmation.getText());
            visible.setText("Changed123");
            visibleConfirmation.setText("Changed123");
            toggle.fire();
            assertTrue(masked.isVisible());
            assertFalse(visible.isVisible());
            assertEquals("Показать", toggle.getText());
            assertEquals("Changed123", masked.getText());
            assertEquals("Changed123", confirmation.getText());
            masked.clear();
            confirmation.clear();
            assertEquals("", visible.getText());
            assertEquals("", visibleConfirmation.getText());
            return null;
        });
    }

    @Test void allFxmlWindowsLoadAndRender() throws Exception {
        fx(()->{
            for(String name: new String[]{"login","main","clients","visits","statistics","users","client-form","visit-form","user-form"}) {
                var view=Views.load(name);
                Scene scene=Views.scene(view.root(),name.contains("form")?560:1320,name.contains("form")?740:860);
                var stage=new Stage();stage.setScene(scene);stage.show();view.root().applyCss();view.root().layout();
                assertNotNull(view.controller(),name);assertTrue(view.root().getBoundsInLocal().getWidth()>0,name);
                assertNotNull(view.root().snapshot(null,null),name);
                screenshot(scene, "empty-"+name);
                stage.close();
            }
            return null;
        });
    }

    @Test @EnabledIfEnvironmentVariable(named="POOL_UI_DB_URL",matches=".+")
    void loadsLiveDataSearchesFiltersAndRendersAllTabs() throws Exception {
        var props=new Properties();props.setProperty("db.url",System.getenv("POOL_UI_DB_URL"));
        props.setProperty("db.username","postgres");props.setProperty("db.password","postgres");
        props.setProperty("db.schema","public");
        var context=new AppContext(new DatabaseManager(props));
        var session=context.auth.login("admin","Admin123".toCharArray());
        var tasks=new FxTasks();
        var view=fx(()->Views.<MainController>load("main"));
        var stage=fx(()->{var s=new Stage();s.setScene(Views.scene(view.root(),1320,860));s.show();view.controller().configure(context,session,tasks,null);return s;});
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(fx(()->view.root().isDisable()) && System.nanoTime()<deadline) Thread.sleep(30);
            assertEquals(8,fx(()->view.controller().clients.size()));
            assertEquals(20,fx(()->view.controller().visits.size()));
            fx(()->{
                var tabs=(TabPane)view.root().lookup("#tabs");
                String[] names={"visits","clients","statistics","users"};
                for(int i=0;i<tabs.getTabs().size();i++){
                    tabs.getSelectionModel().select(i);view.root().applyCss();view.root().layout();screenshot(stage.getScene(),names[i]);
                }
                tabs.getSelectionModel().select(0);
                var pane=tabs.getTabs().getFirst().getContent();
                var search=(TextField)pane.lookup("#searchField");
                var table=(TableView<?>)pane.lookup("#table");
                search.setText("Иванов");assertEquals(3,table.getItems().size());search.clear();
                @SuppressWarnings("unchecked") var filter=(ComboBox<ru.mirea.pool.domain.model.VisitStatus>)pane.lookup("#statusFilter");
                filter.setValue(ru.mirea.pool.domain.model.VisitStatus.CANCELLED);assertEquals(2,table.getItems().size());filter.setValue(null);
                return null;
            });
            // Exercise the actual modal forms and save handlers, not only service methods.
            Platform.runLater(()->{
                var tabs=(TabPane)view.root().lookup("#tabs");tabs.getSelectionModel().select(1);
                button(tabs.getTabs().get(1).getContent(), "+ Добавить клиента").fire();
            });
            Stage clientForm=awaitStage("Новый клиент");
            fx(()->{
                var r=clientForm.getScene().getRoot();
                ((TextField)r.lookup("#firstNameField")).setText("Тестовый");
                ((TextField)r.lookup("#lastNameField")).setText("Интерфейс");
                ((TextField)r.lookup("#phoneField")).setText("+79990000999");
                ((DatePicker)r.lookup("#birthDatePicker")).setValue(java.time.LocalDate.of(2000,2,2));
                ((Button)r.lookup("#saveButton")).fire();return null;
            });
            await(()->!clientForm.isShowing()&&!view.root().isDisable());
            var client=fx(()->view.controller().clients.stream().filter(c->c.getPhone().equals("+79990000999")).findFirst().orElseThrow());
            Platform.runLater(()->{
                var tabs=(TabPane)view.root().lookup("#tabs");tabs.getSelectionModel().select(0);
                button(tabs.getTabs().getFirst().getContent(), "+ Записать на посещение").fire();
            });
            Stage visitForm=awaitStage("Новое посещение");
            await(()->!visitForm.getScene().lookup("#root").isDisable());
            fx(()->{
                var r=visitForm.getScene().getRoot();
                @SuppressWarnings("unchecked") var box=(ComboBox<ru.mirea.pool.domain.model.Client>)r.lookup("#clientBox");box.setValue(client);
                ((DatePicker)r.lookup("#datePicker")).setValue(java.time.LocalDate.now().plusDays(50));
                button(r,"Найти свободное время").fire();return null;
            });
            await(()->!visitForm.getScene().lookup("#root").isDisable());
            fx(()->{
                assertFalse(((Button)visitForm.getScene().lookup("#saveButton")).isDisabled());
                screenshot(visitForm.getScene(),"visit-form");
                ((Button)visitForm.getScene().lookup("#saveButton")).fire();return null;
            });
            await(()->!visitForm.isShowing()&&!view.root().isDisable());
            assertEquals(21,fx(()->view.controller().visits.size()));
            var created=context.visits.searchByClient(client.getId()).getFirst();
            context.visits.deleteVisit(created.getId());context.clients.deleteClient(client.getId());
        } finally {fx(()->{stage.close();return null;});tasks.close();}
    }
    private static Button button(javafx.scene.Node root,String text){
        return root.lookupAll(".button").stream().filter(n->n instanceof Button b&&b.getText().equals(text)).map(n->(Button)n).findFirst().orElseThrow();
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(System.nanoTime()<end){if(fx(condition))return;Thread.sleep(30);}
        fail("UI operation did not finish within 15 seconds");
    }
    private static Stage awaitStage(String title) throws Exception {
        await(()->javafx.stage.Window.getWindows().stream().anyMatch(w->w instanceof Stage s&&title.equals(s.getTitle())));
        return fx(()->javafx.stage.Window.getWindows().stream().filter(w->w instanceof Stage s&&title.equals(s.getTitle())).map(w->(Stage)w).findFirst().orElseThrow());
    }
    private static void screenshot(Scene scene,String name) throws Exception {
        scene.getRoot().applyCss();scene.getRoot().layout();
        var image=scene.snapshot(null);
        var buffered=new java.awt.image.BufferedImage((int)image.getWidth(),(int)image.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var reader=image.getPixelReader();
        for(int y=0;y<buffered.getHeight();y++) for(int x=0;x<buffered.getWidth();x++) buffered.setRGB(x,y,reader.getArgb(x,y));
        var dir=Path.of("target","screenshots");Files.createDirectories(dir);
        javax.imageio.ImageIO.write(buffered,"png",dir.resolve(name+".png").toFile());
    }
}
