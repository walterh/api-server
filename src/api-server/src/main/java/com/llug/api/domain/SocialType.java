package com.llug.api.domain;

import java.util.ArrayList;
import java.util.List;

import com.wch.commons.utils.Utils;

public enum SocialType {
    Twitter, Facebook, Google, Tumblr, Flickr, Linkedin;

    // this must be int (not Integer), else comparisons == will fail.
    public int sourceValue() {
        switch (this) {
        case Twitter:
            return 1;
        case Facebook:
            return 2;
        case Google:
            return 3;
        case Tumblr:
            return 4;
        case Flickr:
            return 5;
        case Linkedin:
            return 6;
        }
        throw new IllegalArgumentException("Illegal social type:" + this);
    }

    public String sourceValueAsString() {
        return String.valueOf(sourceValue());
    }

    public static SocialType valueOf(int value) {
        switch (value) {
        case 1:
            return Twitter;
        case 2:
            return Facebook;
        case 3:
            return Google;
        case 4:
            return Tumblr;
        case 5:
            return Flickr;
        case 6:
            return Linkedin;
        }
        throw new IllegalArgumentException("Illegal social type value:" + value);
    }

    public static SocialType parse(String value) {
        try {
            int sourceVal = sourceValue(value);
            return valueOf(sourceVal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int sourceValue(String value) {
        if (Twitter.toString().equalsIgnoreCase(value) || Integer.valueOf(Twitter.sourceValue()).toString().equals(value)) {
            return Twitter.sourceValue();
        } else if (Facebook.toString().equalsIgnoreCase(value) || Integer.valueOf(Facebook.sourceValue()).toString().equals(value)) {
            return Facebook.sourceValue();
        } else if (Google.toString().equalsIgnoreCase(value) || Integer.valueOf(Google.sourceValue()).toString().equals(value)) {
            return Google.sourceValue();
        } else if (Tumblr.toString().equalsIgnoreCase(value) || Integer.valueOf(Tumblr.sourceValue()).toString().equals(value)) {
            return Tumblr.sourceValue();
        } else if (Flickr.toString().equalsIgnoreCase(value) || Integer.valueOf(Flickr.sourceValue()).toString().equals(value)) {
            return Flickr.sourceValue();
        } else if (Linkedin.toString().equalsIgnoreCase(value) || Integer.valueOf(Linkedin.sourceValue()).toString().equals(value)) {
            return Linkedin.sourceValue();
        }

        throw new IllegalArgumentException("No social type value: " + value);
    }

    public static List<String> asStringList(final List<SocialType> l) {
        List<String> l2 = null;

        if (!Utils.isNullOrEmpty(l)) {
            l2 = new ArrayList<String>();

            for (SocialType st : l) {
                l2.add(st.sourceValueAsString());
            }
        }

        return l2;
    }
}