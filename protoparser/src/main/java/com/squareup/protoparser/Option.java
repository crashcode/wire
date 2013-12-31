// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static com.squareup.protoparser.Utils.appendIndented;

public final class Option {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> optionsAsMap(List<Option> options) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : options) {
      String name = option.getName();
      Object value = option.getValue();

      if (value instanceof String || value instanceof List) {
        map.put(name, value);
      } else if (value instanceof Option) {
        Map<String, Object> newMap = optionsAsMap(Arrays.asList((Option) value));

        Object oldValue = map.get(name);
        if (oldValue instanceof Map) {
          Map<String, Object> oldMap = (Map<String, Object>) oldValue;
          // Existing nested maps are immutable. Make a mutable copy, update, and replace.
          oldMap = new LinkedHashMap<String, Object>(oldMap);
          oldMap.putAll(newMap);
          map.put(name, oldMap);
        } else {
          map.put(name, newMap);
        }
      } else if (value instanceof Map) {
        Object oldValue = map.get(name);
        if (oldValue instanceof Map) {
          ((Map<String, Object>) oldValue).putAll((Map<String, Object>) value);
        } else {
          map.put(name, value);
        }
      } else {
        throw new AssertionError("Option value must be String, Option, List, or Map<String, ?>");
      }
    }
    return unmodifiableMap(map);
  }

  private final String name;
  private final Object value;

  public Option(String name, Object value) {
    if (name == null) throw new NullPointerException("name");
    if (value == null) throw new NullPointerException("value");

    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Option)) return false;

    Option that = (Option) other;
    return name.equals(that.name) && value.equals(that.value);
  }

  @Override public int hashCode() {
    return name.hashCode() + (37 * value.hashCode());
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    if (value instanceof String) {
      String stringValue = (String) value;
      builder.append(name).append(" = \"").append(escape(stringValue)).append('"');
    } else if (value instanceof Option) {
      Option optionValue = (Option) value;
      builder.append('(').append(name).append(").").append(optionValue.toString());
    } else if (value instanceof List) {
      builder.append(name).append(" = [\n");
      //noinspection unchecked
      List<Option> optionList = (List<Option>) value;
      for (int i = 0, count = optionList.size(); i < count; i++) {
        String endl = (i < count - 1) ? "," : "";
        appendIndented(builder, optionList.get(i).toString() + endl);
      }
      builder.append(']');
    } else {
      throw new IllegalStateException("Unknown value type " + value.getClass().getCanonicalName());
    }
    return builder.toString();
  }

  public String toDeclaration() {
    return "option " + toString() + ";\n";
  }

  static String escape(String string) {
    return string.replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }
}
