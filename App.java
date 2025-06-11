import javax.swing.*;

class App{
    public static void main(String[] args) throws Exception
    {
    int BoardWidth = 360;
    int BoardHeight = 640;

    JFrame frame = new JFrame("Flappy bird");
  //  frame.setVisible(true);
    frame.setSize(BoardWidth,BoardHeight);
    frame.setLocationRelativeTo(null);
    frame.setResizable(false);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    FlappyBird FB = new FlappyBird();
    frame.add(FB);
    frame.pack();
    FB.requestFocus();
    frame.setVisible(true);
    }
}