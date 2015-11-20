import dorkbox.util.ActionHandler;
import dorkbox.util.growl.Growl;
import dorkbox.util.growl.Pos;

/**
 *
 */
public
class GrowlTest {

    public static
    void main(String[] args) {
        Growl growl;

        int count = 3;

        for (int i = 0; i < count; i++) {
            growl = Growl.create()
                         .title("Growl title " + i)
                         .text("This is a growl notification popup message This is a growl notification popup message This is a growl notification popup message")
                         .hideAfter(50000)
                         .position(Pos.TOP_RIGHT)
//                       .setScreen(0)
                         .darkStyle()
                         .shake(1300, 4)
//                       .shake(1300, 10)
//                       .hideCloseButton()
                         .onAction(new ActionHandler<Growl>() {
                                   @Override
                                   public
                                   void handle(final Growl arg0) {
                                       System.out.println("Notification clicked on!");
                                   }
                               });
            growl.showWarning();
//            growl.show();

        }
    }
}
