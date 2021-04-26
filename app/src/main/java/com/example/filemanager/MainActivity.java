package com.example.filemanager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //buttons
    private Button deleteButton;
    private Button renameButton;
    private Button copyButton;
    private Button pasteButton;

    //listview
    private ListView listView;

    //text adapter
    private TextAdapter textAdapter1;

    private File[] files;
    private List<String> filesList;
    private int filesFoundCount;
    //String
    private String copyPath;
    private int selectedItemIndex;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }

    private boolean[] select;

    private Button refreshButton;
    private File dir;
    private String currentPath;
    private boolean isFMInitialized;
    private boolean isLongClick;

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if (!isFMInitialized) {
            currentPath = String.valueOf(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
            final TextView pathOutput = findViewById(R.id.pathOutput);

            listView = findViewById(R.id.fListView);
            textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);



            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    dir = new File(currentPath);
                    files = dir.listFiles();
                    filesFoundCount = files.length;
                    select = new boolean[filesFoundCount];
                    textAdapter1.setSelect(select);
                    filesList = new ArrayList<>();
                    filesList.clear();
                    for (int i = 0; i < filesFoundCount; i++) {
                        filesList.add(String.valueOf(files[i].getAbsoluteFile()));
                    }

                    textAdapter1.setData(filesList);
                }
            });

            refreshButton.callOnClick();

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)) {
                        return;
                    }
                    currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                    refreshButton.callOnClick();
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                             if(!isLongClick) {
                                 if (files[position].isDirectory()) {
                                     currentPath = files[position].getAbsolutePath();
                                     refreshButton.callOnClick();
                                 }
                             }
                        }
                    }, 50);

                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick = true;
                    select[position] = !select[position];
                    textAdapter1.setSelect(select);
                    int selectionCount = 0;
                    for (boolean aSelection : select) {
                        if (aSelection){
                            selectionCount++;
                        }
                    }
                    if (selectionCount > 0) {
                        if(selectionCount==1) {
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                            if(!files[selectedItemIndex].isDirectory()) {
                                findViewById(R.id.copy).setVisibility(View.VISIBLE);
                            }
                        } else {
                            findViewById(R.id.copy).setVisibility(View.GONE);
                            findViewById(R.id.rename).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottombar).setVisibility(View.VISIBLE);
                    }else {
                        findViewById(R.id.bottombar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                          isLongClick = false;
                        }
                    }, 1000);
                    return false;
                }
            });

            deleteButton = findViewById(R.id.button1);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Are you sure you want to delete this?");
                    deleteDialog.setPositiveButton("Yes", (dialog, which) -> {
                        for (int i=0; i<files.length; i++) {
                            if (select[i]) {
                                deleteFileFolder(files[i]);
                                select[i] = false;
                            }
                        }
                        refreshButton.callOnClick();
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            refreshButton.callOnClick();
                        }
                    });
                    deleteDialog.show();
                }
            });

            final Button createNewFolder = findViewById(R.id.newFolder);
            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("New Folder");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(currentPath +"/"+input.getText());
                                    if (!newFolder.exists()) {
                                        newFolder.mkdir();
                                        refreshButton.callOnClick();
                                    }
                                }
                            });
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    newFolderDialog.show();
                }
            });

            renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename file to: ");
                    final EditText renameInput = new EditText(MainActivity.this);
                    String renamePath = files[selectedItemIndex].getAbsolutePath();
                    renameInput.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    renameInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(renameInput);
                    renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String s = new File(renamePath).getParent() + "/" +renameInput.getText();
                            File newFile = new File(s);
                            new File(renamePath).renameTo(newFile);
                            refreshButton.callOnClick();
                            select = new boolean[files.length];
                            textAdapter1.setSelect(select);
                        }
                    });
                    renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            refreshButton.callOnClick();
                        }
                    });
                    renameDialog.show();
                }
            });

            copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = files[selectedItemIndex].getAbsolutePath();
                    select = new boolean[files.length];
                    textAdapter1.setSelect(select);
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });

            pasteButton =  findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pasteButton.setVisibility(View.GONE);
                    String dstPath = currentPath + copyPath.substring(copyPath.lastIndexOf('/'));
                   copy(new File(copyPath), new File(dstPath));
                    refreshButton.callOnClick();
                }
            });

            isFMInitialized=true;
        } else {
            refreshButton.callOnClick();
        }
    }

    private void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int len;
            while((len = in.read(buffer))>0) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    class TextAdapter extends BaseAdapter {

        private List<String> data = new ArrayList<>();

        private boolean[] select;
        public void setData(List<String> data) {
            if(data != null) {
                this.data.clear();
                if(data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelect(boolean[] select) {
            if (select != null) {
                this.select = new boolean[select.length];
                for (int i=0; i<select.length; i++){
                    this.select[i]=select[i];
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)) );
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));

            if (select!=null){
                if (select[position]) {
                   holder.info.setBackgroundColor(Color.argb(100,8,8,8));
                } else {
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }

        class ViewHolder{
            TextView info;

            ViewHolder(TextView info) {
                this.info = info;
            }
        }
    }

    private static final int REQUEST_PERMISSIONS = 123;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private  static final int PERMISSIONS_COUNT = 2;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermissionsDenied() {
            int p = 0;
            while (p < PERMISSIONS_COUNT) {
                if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                p++;
            }
        return false;
    }



    private void deleteFileFolder(File fileFold) {
        if(fileFold.isDirectory()) {
            if(fileFold.list().length==0){
                fileFold.delete();
            }else {
                String files[] = fileFold.list();
                for (String temp:files) {
                    File FileToDelete = new File(fileFold, temp);
                    deleteFileFolder(FileToDelete);
                }
                if (fileFold.list().length==0) {
                    fileFold.delete();
                }
            }
        } else {
            fileFold.delete();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions, final int[] giveResults) {
        super.onRequestPermissionsResult(requestCode, permissions, giveResults);
        if (requestCode == REQUEST_PERMISSIONS && giveResults.length >0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (arePermissionsDenied()) {
                    ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                    recreate();
                } else {
                    onResume();
                }
            }
        }
    }
}