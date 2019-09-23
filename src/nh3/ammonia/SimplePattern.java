package nh3.ammonia;

import java.util.List;

public class SimplePattern {


  final public String beforeText;
  final public String afterText;
  final public List<String> beforeTextPattern;
  final public List<String> afterTextPattern;

  public SimplePattern(final String beforeText, final String afterText) {
    this.beforeText = beforeText;
    this.afterText = afterText;
    this.beforeTextPattern = StringUtility.split(beforeText);
    this.afterTextPattern = StringUtility.split(afterText);
  }

  @Override
  final public boolean equals(final Object o) {

    if (!(o instanceof SimplePattern)) {
      return false;
    }

    final SimplePattern target = (SimplePattern) o;
    return this.beforeText.equals(target.beforeText) && this.afterText.equals(target.afterText);
  }

  @Override
  final public int hashCode() {
    return this.beforeText.hashCode() + this.afterText.hashCode();
  }
}
